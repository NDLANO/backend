/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import java.io.RandomAccessFile
import java.net.Socket
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import scala.util.Try

case class SharedContainerInfo(containerId: String, data: Map[String, String])

object SharedContainer {
  // Disable Ryuk so testcontainers doesn't kill shared containers when individual JVM forks exit.
  // We handle container lifecycle ourselves via refcounting + docker rm.
  System.setProperty("testcontainers.reuse.enable", "true"): Unit
  System.setProperty("testcontainers.ryuk.disabled", "true"): Unit

  private val coordDir: Path = Paths.get(System.getProperty("java.io.tmpdir"), "ndla-test-containers")

  private case class LocalCache(refCount: AtomicInteger, info: SharedContainerInfo)
  private val localCaches: ConcurrentHashMap[String, LocalCache]                    = new ConcurrentHashMap()
  private val nameLocks: ConcurrentHashMap[String, AnyRef]                          = new ConcurrentHashMap()
  private val shutdownHooksRegistered: ConcurrentHashMap[String, java.lang.Boolean] = new ConcurrentHashMap()

  private val staleThresholdMs: Long = 2 * 60 * 60 * 1000 // 2 hours

  private def lockFile(name: String): Path     = coordDir.resolve(s"$name.lock")
  private def infoFile(name: String): Path     = coordDir.resolve(s"$name.info")
  private def refCountFile(name: String): Path = coordDir.resolve(s"$name.refcount")

  private def getNameLock(name: String): AnyRef = nameLocks.computeIfAbsent(name, _ => new AnyRef)

  def acquire(
      name: String,
      healthCheckPort: Int,
      startContainer: () => SharedContainerInfo,
      healthCheck: SharedContainerInfo => Boolean = null,
  ): SharedContainerInfo = {
    val check: SharedContainerInfo => Boolean =
      if (healthCheck != null) healthCheck
      else { info =>
        val host = info.data.getOrElse("host", "localhost")
        val port = info.data.get("port").flatMap(p => Try(p.toInt).toOption).getOrElse(healthCheckPort)
        isReachable(host, port)
      }
    getNameLock(name).synchronized {
      val existing = localCaches.get(name)
      if (existing != null) {
        existing.refCount.incrementAndGet()
        return existing.info
      }

      val info = acquireAcrossJvms(name, startContainer, check)
      localCaches.put(name, LocalCache(new AtomicInteger(1), info))
      registerShutdownHook(name)
      info
    }
  }

  def release(name: String): Unit = {
    getNameLock(name).synchronized {
      val cached = localCaches.get(name)
      if (cached == null) return

      val remaining = cached.refCount.decrementAndGet()
      if (remaining <= 0) {
        localCaches.remove(name)
        decrementGlobalRefCount(name, cached.info.containerId)
      }
    }
  }

  private def acquireAcrossJvms(
      name: String,
      startContainer: () => SharedContainerInfo,
      healthCheck: SharedContainerInfo => Boolean,
  ): SharedContainerInfo = {
    Files.createDirectories(coordDir)
    val raf  = new RandomAccessFile(lockFile(name).toFile, "rw")
    val lock = raf.getChannel.lock()
    try {
      val infoPath     = infoFile(name)
      val refCountPath = refCountFile(name)

      // Check for stale state
      if (Files.exists(refCountPath)) {
        val lastModified = Files.getLastModifiedTime(refCountPath).toMillis
        if (System.currentTimeMillis() - lastModified > staleThresholdMs) {
          cleanupStale(name)
        }
      }

      // Try to reuse existing container
      val existingInfo = readInfoFile(infoPath)
      val reusable     = existingInfo.exists(healthCheck)

      val info =
        if (reusable) {
          existingInfo.get
        } else {
          // Clean up any dead container info
          if (existingInfo.isDefined) {
            stopContainer(existingInfo.get.containerId)
            Files.deleteIfExists(infoPath): Unit
            Files.deleteIfExists(refCountPath): Unit
          }
          val newInfo = startContainer()
          writeInfoFile(infoPath, newInfo)
          newInfo
        }

      incrementGlobalRefCount(refCountPath)
      info
    } finally {
      lock.release()
      raf.close()
    }
  }

  private def decrementGlobalRefCount(name: String, containerId: String): Unit = {
    Files.createDirectories(coordDir)
    val raf  = new RandomAccessFile(lockFile(name).toFile, "rw")
    val lock = raf.getChannel.lock()
    try {
      val refCountPath = refCountFile(name)
      val current      = readRefCount(refCountPath)
      val newCount     = Math.max(0, current - 1)
      if (newCount == 0) {
        stopContainer(containerId)
        Files.deleteIfExists(infoFile(name)): Unit
        Files.deleteIfExists(refCountPath): Unit
      } else {
        writeRefCount(refCountPath, newCount)
      }
    } finally {
      lock.release()
      raf.close()
    }
  }

  private def cleanupStale(name: String): Unit = {
    val existingInfo = readInfoFile(infoFile(name))
    existingInfo.foreach(info => stopContainer(info.containerId))
    Files.deleteIfExists(infoFile(name)): Unit
    Files.deleteIfExists(refCountFile(name)): Unit
  }

  private def registerShutdownHook(name: String): Unit = {
    if (shutdownHooksRegistered.putIfAbsent(name, true) == null) {
      Runtime
        .getRuntime
        .addShutdownHook(
          new Thread(() => {
            val cached = localCaches.get(name)
            if (cached != null && cached.refCount.get() > 0) {
              cached.refCount.set(0)
              localCaches.remove(name)
              decrementGlobalRefCount(name, cached.info.containerId)
            }
          })
        )
    }
  }

  private def isReachable(host: String, port: Int): Boolean = {
    Try {
      val socket = new Socket()
      socket.connect(new java.net.InetSocketAddress(host, port), 2000)
      socket.close()
    }.isSuccess
  }

  private def stopContainer(containerId: String): Unit = {
    Try {
      val process = new ProcessBuilder("docker", "rm", "-f", containerId).redirectErrorStream(true).start()
      process.waitFor()
    }: Unit
  }

  // Simple line-based serialization for SharedContainerInfo
  private def writeInfoFile(path: Path, info: SharedContainerInfo): Unit = {
    val lines = s"containerId=${info.containerId}" +: info
      .data
      .map { case (k, v) =>
        s"$k=$v"
      }
      .toSeq
    Files.writeString(path, lines.mkString("\n"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING): Unit
  }

  private def readInfoFile(path: Path): Option[SharedContainerInfo] = {
    if (!Files.exists(path)) return None
    Try {
      val lines = Files.readString(path).split("\n").toSeq
      val pairs = lines
        .map { line =>
          val idx = line.indexOf('=')
          (line.substring(0, idx), line.substring(idx + 1))
        }
        .toMap
      val containerId = pairs("containerId")
      SharedContainerInfo(containerId, pairs - "containerId")
    }.toOption
  }

  private def readRefCount(path: Path): Int = {
    if (!Files.exists(path)) 0
    else Try(Files.readString(path).trim.toInt).getOrElse(0)
  }

  private def writeRefCount(path: Path, count: Int): Unit = {
    Files.writeString(path, count.toString, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING): Unit
  }

  private def incrementGlobalRefCount(path: Path): Unit = {
    writeRefCount(path, readRefCount(path) + 1)
  }
}
