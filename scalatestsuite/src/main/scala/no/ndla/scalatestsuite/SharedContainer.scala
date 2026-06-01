/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import org.testcontainers.utility.TestcontainersConfiguration

import java.io.RandomAccessFile
import java.net.Socket
import java.nio.file.{Files, Path, Paths, StandardOpenOption}
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import scala.util.Try

/** Coordination payload persisted across JVMs. [[data]] is the typed container info produced by the owning suite */
case class SharedContainerInfo[O](containerId: String, data: O)

object SharedContainerInfo {
  given encoder[O](using Encoder[O]): Encoder[SharedContainerInfo[O]] = deriveEncoder
  given decoder[O](using Decoder[O]): Decoder[SharedContainerInfo[O]] = deriveDecoder
}

object SharedContainer {
  TestcontainersConfiguration.getInstance().getUserProperties.setProperty("testcontainers.reuse.enable", "true"): Unit

  private val coordDir: Path = Paths.get(System.getProperty("java.io.tmpdir"), "ndla-test-containers")

  private case class LocalCache(refCount: AtomicInteger, info: SharedContainerInfo[?])
  private val localCaches: ConcurrentHashMap[String, LocalCache]                    = new ConcurrentHashMap()
  private val nameLocks: ConcurrentHashMap[String, AnyRef]                          = new ConcurrentHashMap()
  private val shutdownHooksRegistered: ConcurrentHashMap[String, java.lang.Boolean] = new ConcurrentHashMap()

  private def lockFile(name: String): Path     = coordDir.resolve(s"$name.lock")
  private def infoFile(name: String): Path     = coordDir.resolve(s"$name.info")
  private def refCountFile(name: String): Path = coordDir.resolve(s"$name.refcount")

  private def getNameLock(name: String): AnyRef = nameLocks.computeIfAbsent(name, _ => new AnyRef)

  def acquire[O: {Encoder, Decoder}](
      name: String,
      startContainer: => SharedContainerInfo[O],
      healthCheck: O => Boolean,
  ): SharedContainerInfo[O] = {
    getNameLock(name).synchronized {
      val existing = localCaches.get(name)
      if (existing != null) {
        existing.refCount.incrementAndGet()
        // Safe: a given container name is always acquired with the same O.
        return existing.info.asInstanceOf[SharedContainerInfo[O]]
      }

      val info = acquireAcrossJvms(name, startContainer, healthCheck)
      localCaches.put(name, LocalCache(new AtomicInteger(1), info))
      registerShutdownHook(name)
      info
    }
  }

  def isReachable(host: String, port: Int): Boolean = {
    Try {
      val socket = new Socket()
      socket.connect(new java.net.InetSocketAddress(host, port), 2000)
      socket.close()
    }.isSuccess
  }

  private def acquireAcrossJvms[O: {Encoder, Decoder}](
      name: String,
      startContainer: => SharedContainerInfo[O],
      healthCheck: O => Boolean,
  ): SharedContainerInfo[O] = {
    Files.createDirectories(coordDir)
    val raf  = new RandomAccessFile(lockFile(name).toFile, "rw")
    val lock = raf.getChannel.lock()
    try {
      val infoPath     = infoFile(name)
      val refCountPath = refCountFile(name)

      val existingInfo = readInfoFile[O](infoPath)
      val info         = existingInfo match {
        case Some(exInfo) if healthCheck(exInfo.data) => exInfo
        case _                                        =>
          existingInfo.foreach { stale =>
            stopContainer(stale.containerId)
            Files.deleteIfExists(infoPath): Unit
            Files.deleteIfExists(refCountPath): Unit
          }
          val newInfo = startContainer
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

  private def stopContainer(containerId: String): Unit = {
    Try {
      val process = new ProcessBuilder("docker", "rm", "-f", containerId).redirectErrorStream(true).start()
      process.waitFor()
    }: Unit
  }

  private def writeInfoFile[O: Encoder](path: Path, info: SharedContainerInfo[O]): Unit = {
    Files.writeString(path, info.asJson.noSpaces, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING): Unit
  }

  private def readInfoFile[O: Decoder](path: Path): Option[SharedContainerInfo[O]] = {
    if (!Files.exists(path)) None
    else Try(Files.readString(path)).toOption.flatMap(content => decode[SharedContainerInfo[O]](content).toOption)
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
