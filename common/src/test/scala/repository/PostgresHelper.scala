package io.github.sergeiionin.contractsregistrator
package repository

import cats.syntax.option.*
import cats.effect.IO
import cats.effect.Resource
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
import cats.effect.testing.specs2.CatsEffect
import org.specs2.mutable.Specification
import org.specs2.matcher.ShouldMatchers
import org.specs2.mutable.*
import skunk.Session
import skunk.codec.all._
import skunk.*
import skunk.implicits.*
import natchez.Trace.Implicits.noop
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}
import domain.Contract

trait PostgresHelper[F[_]]:
  lazy val container: PostgreSQLContainer = PostgreSQLContainer()

  lazy val postgresResource: Resource[IO, PostgreSQLContainer] =
    Resource.make(IO.delay {
      container.start()
      container
    })(c => {
      IO.delay(c.stop())
    })

  def initPostgres(session: Session[IO]): IO[Unit] =
    session.execute(
      sql"""CREATE TABLE contracts(
        subject VARCHAR NOT NULL,
        version INTEGER NOT NULL,
        id INTEGER NOT NULL,
        schema TEXT NOT NULL,
        PRIMARY KEY (subject, version));""".command).void

  def initPostgres(sessionR: Resource[IO, Session[IO]]): IO[Unit] =
    sessionR.use { session =>
      session.execute(
        sql"""CREATE TABLE contracts(
            subject VARCHAR NOT NULL,
            version INTEGER NOT NULL,
            id INTEGER NOT NULL,
            schema TEXT NOT NULL,
            PRIMARY KEY (subject, version));""".command).void
    }

  def sessionSingleResource(container: PostgreSQLContainer): Resource[IO, Session[IO]] =
    Session.single[IO](
      host = container.containerIpAddress,
      port = container.firstMappedPort,
      user = container.username,
      password = Some(container.password),
      database = container.databaseName
    )

  def sessionPooledResource(container: PostgreSQLContainer): Resource[IO, Resource[IO, Session[IO]]] =
    Session.pooled[IO](
      host = container.containerIpAddress,
      port = container.firstMappedPort,
      user = container.username,
      password = container.password.some,
      database = container.databaseName,
      max = 10
    ) 