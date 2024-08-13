package io.github.sergeiionin.contractsregistrator
package repository

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

class ContractsRepositoryPostgresImplSpec extends Specification with CatsEffect:

  val container: PostgreSQLContainer = PostgreSQLContainer()
  
  def initPostgres(session: Session[IO]): IO[Unit] = 
    session.execute(sql"""CREATE TABLE contracts(
      subject VARCHAR NOT NULL,
      version INTEGER NOT NULL,
      id INTEGER NOT NULL,
      schema TEXT NOT NULL,
      PRIMARY KEY (subject, version));""".command).void

  val testSubject: String = "testSubject"
  val testId1: Int = 3
  val testId2: Int = testId1+1
  val testVersion1: Int = 1
  val testVersion2: Int = testVersion1+1
  val testSchema1: String = "testSchema_1"
  val testSchema2: String = "testSchema_2"
  val testContractV1: Contract = Contract(testSubject, testVersion1, testId1, testSchema1)
  val testContractV2: Contract = Contract(testSubject, testVersion2, testId2, testSchema2)
  
  "ContractsRepositoryPostgresImpl" should {
    "perform CRUD operations correctly" in {
      val postgresResource: Resource[IO, PostgreSQLContainer] =
        Resource.make(IO.delay {
          container.start()
          container
        })(c => {
          IO.delay(c.stop())
        })
      
      
      def sessionResource(container: PostgreSQLContainer): Resource[IO, Session[IO]] =
        Session.single[IO](
          host = container.containerIpAddress,
          port = container.firstMappedPort,
          user = container.username,
          password = Some(container.password),
          database = container.databaseName
        )

      given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

      (for
        postgres <- postgresResource
        session <- sessionResource(postgres)
        repo <- ContractsRepository.make[IO](session)
      yield (session, repo)).use { (s, repository) =>
        for
          _         <- initPostgres(s)
          _         <- repository.save(testContractV1)
          _         <- repository.save(testContractV2)
          resOpt1   <- repository.get(testSubject, 1)
          _         <- IO.whenA(resOpt1.isEmpty)(IO.raiseError(new RuntimeException("Contract not found")))
          res1      =  resOpt1.get
          _         =  res1 must beEqualTo(testContractV1)
          versionsS <- repository.getAllVersionsForSubject(testSubject)
          versions  <- versionsS.compile.toList
          _         =  versions must beEqualTo(List(1, 2))
          _         <- repository.delete(testSubject, 1)
          resOpt1   <- repository.get(testSubject, 1)
          _         =  resOpt1 must beNone
        yield true
      }
    }
  }