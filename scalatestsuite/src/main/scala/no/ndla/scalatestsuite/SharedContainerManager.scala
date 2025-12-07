/*
 * Part of NDLA scalatestsuite
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.scalatestsuite

import com.github.dockerjava.api.command.InspectContainerResponse
import com.github.dockerjava.api.model.ExposedPort
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

import java.net.Socket
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse.BodyHandlers
import java.nio.channels.FileChannel
import java.nio.charset.StandardCharsets
import java.nio.file.StandardOpenOption.{CREATE, TRUNCATE_EXISTING, WRITE}
import java.nio.file.{Files, Path, Paths}
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.sql.DriverManager
import scala.concurrent.blocking
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

object SharedContainerManager {
  sys.props.update("testcontainers.reuse.enable", "true")
  sys.props.update("TESTCONTAINERS_RYUK_DISABLED", "true")
  ensureReuseConfig()

  private val removeContainersOnRelease =
    sys.env.get("NDLA_TESTCONTAINERS_REMOVE").exists(_.equalsIgnoreCase("true"))
  private val shutdownHookRegistered = new AtomicBoolean(false)

  private val dockerClient = DockerClientFactory.instance().client()
  private val baseDir: Path = Paths.get(sys.props.getOrElse("java.io.tmpdir", "/tmp"), "ndla-testcontainers")
  Files.createDirectories(baseDir)
  ensureReuseConfig()
  if (removeContainersOnRelease) registerShutdownHook()

  case class Managed[C](container: C, release: () => Unit)

  private case class ContainerRuntime(id: String, host: String, port: Int)
  private case class UsageLock(channel: FileChannel, lock: java.nio.channels.FileLock) {
    def release(): Unit = {
      lock.release()
      channel.close()
    }
  }

  private val postgresName      = sys.env.getOrElse("NDLA_POSTGRES_CONTAINER_NAME", "ndla-test-postgres")
  private val elasticsearchName = sys.env.getOrElse("NDLA_ELASTIC_CONTAINER_NAME", "ndla-test-elasticsearch")

  def acquirePostgres(
      version: String,
      username: String,
      password: String,
      databaseName: String,
      schemaName: String
  ): Try[Managed[PostgreSQLContainer]] =
    Try {
      val usageLock = acquireUsageLock("postgres")
      try {
        val runtime = withLock("postgres") {
          val r = ensurePostgresRunning(version, username, password, databaseName)
          waitForPort(r)
          resetPostgresSchema(r, username, password, databaseName, schemaName)
          incrementCounter("postgres"): Unit
          r
        }
        managedPostgresContainer(runtime, username, password, databaseName, schemaName, usageLock)
      } catch {
        case t: Throwable =>
          usageLock.release()
          throw t
      }
    }

  def acquireElasticsearch(
      image: String,
      envImageOverride: Option[String]
  ): Try[Managed[ElasticsearchContainer]] =
    Try {
      val usageLock = acquireUsageLock("elasticsearch")
      try {
        val runtime = withLock("elasticsearch") {
          val r = ensureElasticsearchRunning(image, envImageOverride)
          waitForPort(r)
          clearElasticsearch(r)
          incrementCounter("elasticsearch"): Unit
          r
        }
        managedElasticsearchContainer(runtime, usageLock)
      } catch {
        case t: Throwable =>
          usageLock.release()
          throw t
      }
    }

  private def managedPostgresContainer(
      runtime: ContainerRuntime,
      username: String,
      password: String,
      databaseName: String,
      schemaName: String,
      usageLock: UsageLock
  ): Managed[PostgreSQLContainer] = {
    val released  = new AtomicBoolean(false)
    lazy val release: () => Unit = () =>
      if (released.compareAndSet(false, true)) {
        withLock("postgres") {
          val remaining = decrementCounter("postgres")
          if (remaining == 0) {
            Try(resetPostgresSchema(runtime, username, password, databaseName, schemaName)).getOrElse(())
          }
        }
        usageLock.release()
      }
    val container =
      new SharedPostgreSQLContainer(runtime.host, runtime.port, username, password, databaseName, release)
    Managed(container, release)
  }

  private def managedElasticsearchContainer(runtime: ContainerRuntime, usageLock: UsageLock): Managed[ElasticsearchContainer] = {
    val released  = new AtomicBoolean(false)
    lazy val release: () => Unit = () =>
      if (released.compareAndSet(false, true)) {
        withLock("elasticsearch") {
          val remaining = decrementCounter("elasticsearch")
          if (remaining == 0) {
            Try(clearElasticsearch(runtime)).getOrElse(())
          }
        }
        usageLock.release()
      }
    val container = new SharedElasticsearchContainer(runtime.host, runtime.port, release)
    Managed(container, release)
  }

  private def ensurePostgresRunning(
      version: String,
      username: String,
      password: String,
      databaseName: String
  ): ContainerRuntime = {
    val expectedImage = s"postgres:$version"
    findContainer(postgresName) match {
      case Some(inspected) if isCompatible(inspected, expectedImage) && isRunning(inspected) =>
        runtimeFromInspect(inspected, 5432)
      case Some(inspected) if isCompatible(inspected, expectedImage) =>
        dockerClient.startContainerCmd(inspected.getId).exec()
        waitForPort(runtimeFromInspect(inspected, 5432))
        runtimeFromInspect(dockerClient.inspectContainerCmd(inspected.getId).exec(), 5432)
      case Some(_) =>
        stopAndRemoveContainer(postgresName, "postgres")
        startPostgres(expectedImage, username, password, databaseName)
      case None =>
        startPostgres(expectedImage, username, password, databaseName)
    }
  }

  private def ensureElasticsearchRunning(
      defaultImage: String,
      envImageOverride: Option[String]
  ): ContainerRuntime = {
    val expectedImage = envImageOverride.getOrElse(defaultImage)
    findContainer(elasticsearchName) match {
      case Some(inspected) if isCompatible(inspected, expectedImage) && isRunning(inspected) =>
        runtimeFromInspect(inspected, 9200)
      case Some(inspected) if isCompatible(inspected, expectedImage) =>
        dockerClient.startContainerCmd(inspected.getId).exec()
        waitForPort(runtimeFromInspect(inspected, 9200))
        runtimeFromInspect(dockerClient.inspectContainerCmd(inspected.getId).exec(), 9200)
      case Some(_) =>
        stopAndRemoveContainer(elasticsearchName, "elasticsearch")
        startElasticsearch(expectedImage)
      case None =>
        startElasticsearch(expectedImage)
    }
  }

  private def startPostgres(
      image: String,
      username: String,
      password: String,
      databaseName: String
  ): ContainerRuntime = {
    val container = PgContainer(image.stripPrefix("postgres:"), username, password, databaseName)
    container.withCreateContainerCmdModifier { cmd =>
      cmd.withName(postgresName)
      ()
    }
    container.withReuse(true)
    container.start()
    runtimeFromContainer(container, 5432)
  }

  private def startElasticsearch(image: String): ContainerRuntime = {
    val searchEngineImage = DockerImageName
      .parse(image)
      .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch")

    val container = new ElasticsearchContainer(searchEngineImage) {
      this.setWaitStrategy(new HostPortWaitStrategy().withStartupTimeout(Duration.ofSeconds(100)))
    }
    container.addEnv("xpack.security.enabled", "false")
    container.addEnv("ES_JAVA_OPTS", "-Xms1g -Xmx1g")
    container.addEnv("discovery.type", "single-node")
    container.withCreateContainerCmdModifier { cmd =>
      cmd.withName(elasticsearchName)
      ()
    }
    container.withReuse(true)
    container.start()
    runtimeFromContainer(container, 9200)
  }

  private def runtimeFromContainer(container: PostgreSQLContainer, internalPort: Int): ContainerRuntime =
    ContainerRuntime(container.getContainerId, container.getHost, container.getMappedPort(internalPort))

  private def runtimeFromContainer(container: ElasticsearchContainer, internalPort: Int): ContainerRuntime =
    ContainerRuntime(container.getContainerId, container.getHost, container.getMappedPort(internalPort))

  private def runtimeFromInspect(inspected: InspectContainerResponse, internalPort: Int): ContainerRuntime = {
    val portBinding = Option(inspected.getNetworkSettings.getPorts.getBindings.get(ExposedPort.tcp(internalPort)))
      .flatMap(_.headOption)
      .getOrElse(throw new RuntimeException(s"Could not find port binding for $internalPort"))
    val host = Option(portBinding.getHostIp).filterNot(_.isBlank).filterNot(_ == "0.0.0.0").getOrElse("localhost")
    val port = portBinding.getHostPortSpec.toInt
    ContainerRuntime(inspected.getId, host, port)
  }

  private def isRunning(inspected: InspectContainerResponse): Boolean =
    inspected.getState != null && inspected.getState.getRunning

  private def isCompatible(inspected: InspectContainerResponse, expectedImage: String): Boolean =
    Option(inspected.getConfig)
      .flatMap(cfg => Option(cfg.getImage))
      .exists(img => img.contains(expectedImage))

  private def findContainer(name: String): Option[InspectContainerResponse] = {
    val containers = dockerClient
      .listContainersCmd()
      .withShowAll(true)
      .withNameFilter(List(name).asJava)
      .exec()
      .asScala
    containers.headOption.map(c => dockerClient.inspectContainerCmd(c.getId).exec())
  }

  private def stopAndRemoveContainer(containerName: String, counterName: String): Unit = {
    findContainer(containerName).foreach { inspected =>
      val _ = Try(dockerClient.stopContainerCmd(inspected.getId).exec()).getOrElse(())
      val _ = Try(dockerClient.removeContainerCmd(inspected.getId).withRemoveVolumes(true).exec()).getOrElse(())
    }
    val _ = updateCounter(counterName, 0 - currentCounter(counterName))
    ()
  }

  private def clearElasticsearch(runtime: ContainerRuntime): Unit = {
    val client = HttpClient.newHttpClient()
    val base   = s"http://${runtime.host}:${runtime.port}"
    val _ = Try(
      client.send(HttpRequest.newBuilder(java.net.URI.create(s"$base/_all")).DELETE().build(), BodyHandlers.discarding())
    ).getOrElse(())
    val _ = Try(
      client.send(HttpRequest.newBuilder(java.net.URI.create(s"$base/_template/*")).DELETE().build(), BodyHandlers.discarding())
    ).getOrElse(())
  }

  private def resetPostgresSchema(
      runtime: ContainerRuntime,
      username: String,
      password: String,
      database: String,
      schema: String
  ): Unit = {
    val url      = s"jdbc:postgresql://${runtime.host}:${runtime.port}/$database"
    val deadline = 30.seconds.fromNow
    var lastErr: Option[Throwable] = None
    while (deadline.hasTimeLeft()) {
      Try {
        val conn = DriverManager.getConnection(url, username, password)
        try {
          val stmt = conn.createStatement()
          try {
            stmt.execute(s"DROP SCHEMA IF EXISTS $schema CASCADE")
            stmt.execute(s"CREATE SCHEMA $schema")
            ()
          } finally stmt.close()
        } finally conn.close()
      } match {
        case Success(_) => return
        case Failure(err) =>
          lastErr = Some(err)
          Thread.sleep(200)
      }
    }
    throw lastErr.getOrElse(new RuntimeException(s"Failed to reset schema $schema"))
  }

  private def waitForPort(runtime: ContainerRuntime, timeout: FiniteDuration = 2.minutes): Unit = {
    val deadline = timeout.fromNow
    while (deadline.hasTimeLeft()) {
      if (isPortOpen(runtime.host, runtime.port)) return
      Thread.sleep(200)
    }
    throw new RuntimeException(s"Container ${runtime.id} did not open ${runtime.host}:${runtime.port} in $timeout")
  }

  private def isPortOpen(host: String, port: Int): Boolean =
    Try(new Socket(host, port)).map(_.close()).isSuccess

  private def withLock[T](name: String)(f: => T): T = {
    val lockFile = baseDir.resolve(s"$name.lock")
    val channel  = FileChannel.open(lockFile, CREATE, WRITE)
    val lock     = blocking(channel.lock())
    try f
    finally {
      lock.release()
      channel.close()
    }
  }

  private def acquireUsageLock(name: String): UsageLock = {
    val lockFile = baseDir.resolve(s"$name.usage.lock")
    val channel  = FileChannel.open(lockFile, CREATE, WRITE)
    val lock     = blocking(channel.lock())
    UsageLock(channel, lock)
  }

  private def tryAcquireUsageLock(name: String): Option[UsageLock] = {
    val lockFile = baseDir.resolve(s"$name.usage.lock")
    val channel  = FileChannel.open(lockFile, CREATE, WRITE)
    val lock     = channel.tryLock()
    if (lock == null) {
      channel.close()
      None
    } else Some(UsageLock(channel, lock))
  }

  private def ensureReuseConfig(): Unit = {
    val configPath = Paths.get(sys.props("user.home"), ".testcontainers.properties")
    val content    = "testcontainers.reuse.enable=true\n"
    if (!Files.exists(configPath) || !Files.readString(configPath).contains("testcontainers.reuse.enable=true")) {
      Files.writeString(configPath, content, CREATE, WRITE, TRUNCATE_EXISTING)
      ()
    }
  }

  private def decrementCounter(name: String): Int = updateCounter(name, -1)
  private def incrementCounter(name: String): Int = updateCounter(name, 1)

  private def updateCounter(name: String, delta: Int): Int = {
    val counterFile = baseDir.resolve(s"$name.count")
    val current      = currentCounter(name)
    val next = Math.max(0, current + delta)
    Files.writeString(counterFile, next.toString, CREATE, TRUNCATE_EXISTING)
    next
  }

  private def currentCounter(name: String): Int = {
    val counterFile = baseDir.resolve(s"$name.count")
    if (Files.exists(counterFile))
      Try(Files.readString(counterFile, StandardCharsets.UTF_8).trim.toInt).getOrElse(0)
    else 0
  }

  private def registerShutdownHook(): Unit =
    if (shutdownHookRegistered.compareAndSet(false, true)) {
      Runtime.getRuntime.addShutdownHook(new Thread(() => forceCleanupIfIdle()))
    }

  private def forceCleanupIfIdle(): Unit = {
    if (!removeContainersOnRelease) return
    forceCleanupIfPossible("postgres", postgresName)
    forceCleanupIfPossible("elasticsearch", elasticsearchName)
  }

  private def forceCleanupIfPossible(counterName: String, containerName: String): Unit = {
    if (currentCounter(counterName) == 0) {
      tryAcquireUsageLock(counterName) match {
        case Some(usageLock) =>
          try {
            withLock(counterName) {
              stopAndRemoveContainer(containerName, counterName)
            }
          } finally usageLock.release()
        case None => ()
      }
    }
  }
}

private class SharedPostgreSQLContainer(
    host: String,
    port: Int,
    username: String,
    password: String,
    databaseName: String,
    onRelease: () => Unit
) extends PostgreSQLContainer("postgres:reused") {
  override def getHost: String                         = host
  override def getMappedPort(originalPort: Int): Integer = Integer.valueOf(port)
  override def getDatabaseName: String                 = databaseName
  override def getUsername: String                     = username
  override def getPassword: String                     = password
  override def getJdbcUrl: String                      = s"jdbc:postgresql://$host:$port/$databaseName"
  override def start(): Unit                           = () // already running
  override def stop(): Unit                            = onRelease()
}

private class SharedElasticsearchContainer(
    host: String,
    port: Int,
    onRelease: () => Unit
) extends ElasticsearchContainer(
      DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:placeholder")
    ) {
  override def getHost: String               = host
  override def getHttpHostAddress: String    = s"$host:$port"
  override def getMappedPort(originalPort: Int): Integer = Integer.valueOf(port)
  override def start(): Unit                 = () // already running
  override def stop(): Unit                  = onRelease()
}
