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

class ContractsRepositoryPostgresImplSpec extends Specification with CatsEffect with PostgresHelper[IO]:
  import ContractsRepositoryPostgresImplSpec.given
  
  val logger: Logger[IO] = summon[Logger[IO]]

  val testSubject: String = "testSubject"
  val testId1: Int = 3
  val testId2: Int = testId1+1
  val testVersion1: Int = 1
  val testVersion2: Int = testVersion1+1
  val testSchema1: String = "testSchema_1"
  val testSchema2: String = "testSchema_2"
  val testContractV1: Contract = Contract(testSubject, testVersion1, testId1, testSchema1)
  val testContractV2: Contract = Contract(testSubject, testVersion2, testId2, testSchema2)

  def delete(repo: ContractsRepository[IO], subject: String, version: Int): IO[Unit] =
      repo.delete(subject, version).void

  "ContractsRepositoryPostgresImpl" should {
    "perform CRUD operations correctly" in {
      (for
        postgres <- postgresResource
        sessionR <- sessionPooledResource(postgres)
        repo <- ContractsRepository.make[IO](sessionR)
      yield (sessionR, repo)).use { (sR, repository) => {
          for
            _ <- initPostgres(sR)
            _ <- repository.save(testContractV1)
            _ <- repository.save(testContractV2)
            resOpt1 <- repository.get(testSubject, 1)
            _ <- IO.whenA(resOpt1.isEmpty)(IO.raiseError(new RuntimeException("Contract not found")))
            res1 = resOpt1.get
            _ = res1 must beEqualTo(testContractV1)
            versionsS <- repository.getAllVersionsForSubject(testSubject)
            versions <- versionsS.compile.toList
            _ = versions must beEqualTo(List(1, 2))
            _ <- repository.delete(testSubject, 2)
            resOpt1 <- repository.get(testSubject, 2)
            _ = resOpt1 must beNone
            _ <- repository.save(testContractV2)
            versionsS <- repository.getAllVersionsForSubject(testSubject)
            versions <- versionsS.compile.toList
            _ = versions must beEqualTo(List(1, 2))
            _ <- versionsS.parEvalMapUnordered(10)(version =>
                    repository.delete(testSubject, version)
                 ).compile.drain
            resOpt1 <- repository.get(testSubject, 1)
            resOpt2 <- repository.get(testSubject, 2)
            _ = resOpt1 must beNone
            _ = resOpt2 must beNone
          yield true
        }
      }
    }
  }
  
object ContractsRepositoryPostgresImplSpec:
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
    