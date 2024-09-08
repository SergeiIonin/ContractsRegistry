package io.github.sergeiionin.contractsregistrator
package repository

import scala.util.{Try, Success, Failure}
import cats.syntax.option.*
import cats.effect.IO
import cats.effect.Resource
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{Container, ForAllTestContainer, PostgreSQLContainer}
import cats.effect.testing.specs2.CatsEffect
import org.specs2.mutable.Specification
import org.specs2.matcher.ShouldMatchers
import org.specs2.mutable.*
import skunk.Session
import skunk.codec.all.*
import skunk.*
import skunk.implicits.*
import natchez.Trace.Implicits.noop
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}
import domain.Contract
import com.github.dockerjava.api.DockerClient
import com.github.dockerjava.core.{DefaultDockerClientConfig, DockerClientBuilder}

import org.scalatest.BeforeAndAfterAll

trait PostgresHelper[F[_]]:

  // todo this is a hack for the case when sidecar ryuk-containers weren't terminated after test. Any chance to make it better?
  def after_All(): Unit =
    val dockerClient: DockerClient = DockerClientBuilder
      .getInstance(DefaultDockerClientConfig.createDefaultConfigBuilder().build())
      .build()
    dockerClient.listContainersCmd().exec().forEach { container =>
      if (container.getImage.contains("testcontainers")) {
        val id = container.getId
        println(
          s"removing container: ${container.getNames.toList.mkString(", ")}, id: ${container.getId}")
        Try(dockerClient.stopContainerCmd(id).exec()) match
          case Failure(e) => println(s"error stopping ryuk container: ${e.getMessage}")
          case Success(_) => ()
      }
    }

  def initPostgres(session: Session[IO]): IO[Unit] =
    session
      .execute(sql"""CREATE TABLE contracts(
        subject VARCHAR NOT NULL,
        version INTEGER NOT NULL,
        id INTEGER NOT NULL,
        schema TEXT NOT NULL,
        schematype TEXT NOT NULL,
        PRIMARY KEY (subject, version));""".command)
      .void

  def initPostgres(sessionR: Resource[IO, Session[IO]]): IO[Unit] =
    sessionR.use { session =>
      session
        .execute(sql"""CREATE TABLE contracts(
            subject VARCHAR NOT NULL,
            version INTEGER NOT NULL,
            id INTEGER NOT NULL,
            schema TEXT NOT NULL,
            schematype TEXT NOT NULL,
            PRIMARY KEY (subject, version));""".command)
        .void
    }

  def sessionSingleResource(container: PostgreSQLContainer): Resource[IO, Session[IO]] =
    Session.single[IO](
      host = container.containerIpAddress,
      port = container.firstMappedPort,
      user = container.username,
      password = Some(container.password),
      database = container.databaseName
    )

  def sessionPooledResource(container: Container): Resource[IO, Resource[IO, Session[IO]]] =
    val postgresContainer = container.asInstanceOf[PostgreSQLContainer]
    Session.pooled[IO](
      host = postgresContainer.containerIpAddress,
      port = postgresContainer.firstMappedPort,
      user = postgresContainer.username,
      password = postgresContainer.password.some,
      database = postgresContainer.databaseName,
      max = 10
    )
