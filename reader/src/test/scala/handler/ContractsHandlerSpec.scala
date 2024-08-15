package io.github.sergeiionin.contractsregistrator
package handler

import cats.effect.IO
import cats.effect.Resource
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{ForAllTestContainer, PostgreSQLContainer}
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
import repository.{ContractsRepository, PostgresHelper}
import github.{GitHubClient, GitHubClientTestImpl}

class ContractsHandlerSpec extends Specification with CatsEffect with PostgresHelper[IO]:
  import ContractsHandlerSpec.given

  val logger: Logger[IO] = summon[Logger[IO]]

  val testSubject: String = "testSubject"
  val testId1: Int = 3
  val testId2: Int = testId1 + 1
  val testVersion1: Int = 1
  val testVersion2: Int = testVersion1 + 1
  val testSchema1: String = "testSchema_1"
  val testSchema2: String = "testSchema_2"
  val testContractV1: Contract = Contract(testSubject, testVersion1, testId1, testSchema1)
  val testContractV2: Contract = Contract(testSubject, testVersion2, testId2, testSchema2)
  
  "ContractsHandler" should {
    "add and delete contracts" in {
      (for
        postgres  <- postgresResource
        session   <- sessionPooledResource(postgres)
        _         <- Resource.eval(initPostgres(session))
        repo      <- ContractsRepository.make[IO](session)
        gitClient <- GitHubClient.test[IO]()
        handler   <- ContractsHandler.make[IO](repo, gitClient)
      yield (handler, repo)).use { (h: ContractsHandler[IO], r: ContractsRepository[IO]) => {
          for
            _ <- h.addContract(testContractV1)
            _ <- h.addContract(testContractV2)
            _ <- h.deleteContract(testSubject)
            versionsS <- r.getAllVersionsForSubject(testSubject)
            versions <- versionsS.compile.toList
            _ = versions must beEmpty
          yield true
        }
      }
    }
  }


object ContractsHandlerSpec:
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
