package io.github.sergeiionin.contractsregistrator
package handler

import repository.PostgresHelper

import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import org.scalatest.wordspec.AnyWordSpec
import domain.Contract
import domain.SchemaType.PROTOBUF
import github.{GitHubClient, GitHubClientTest, GitHubClientTestImpl}
import repository.{ContractsRepository, PostgresHelper}

import cats.effect.{IO, Resource}
import cats.effect.unsafe.implicits.global
import com.dimafeng.testcontainers.{Container, ForAllTestContainer, PostgreSQLContainer}
import natchez.Trace.Implicits.noop
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.testcontainers.utility.DockerImageName
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

// fixme move to GitHubServiceSpec
class ContractsHandlerSpec
    extends AnyWordSpec
    with Matchers
    with TestContainerForAll
    with PostgresHelper[IO]:
  import ContractsHandlerSpec.given

  val logger: Logger[IO] = summon[Logger[IO]]

  val testSubject: String = "testSubject"
  val testId1: Int = 3
  val testId2: Int = testId1 + 1
  val testVersion1: Int = 1
  val testVersion2: Int = testVersion1 + 1
  val testSchema1: String = "testSchema_1"
  val testSchema2: String = "testSchema_2"
  val testContractV1: Contract =
    Contract(testSubject, testVersion1, testId1, testSchema1, PROTOBUF)
  val testContractV2: Contract =
    Contract(testSubject, testVersion2, testId2, testSchema2, PROTOBUF)

  override val containerDef = PostgreSQLContainer.Def(
    dockerImageName = DockerImageName.parse("postgres:16.3"),
    databaseName = "testcontainer-scala",
    username = "scala",
    password = "scala"
  )

  override def beforeContainersStop(containers: Containers): Unit = {
    super.beforeContainersStop(containers)
    println("Shutting down ryuk container...")
    after_All()
  }

  "ContractsHandler" should {
    "add, set isMerged status and delete contracts" in {
      withContainers { container =>
        val psqlContainer: PostgreSQLContainer = container.asInstanceOf[PostgreSQLContainer]
        (for
          session   <- sessionPooledResource(psqlContainer)
          _         <- Resource.eval(initPostgres(session))
          repo      <- ContractsRepository.make[IO](session)
          gitClient <- GitHubClientTest.test[IO]()
          handler   <- ContractsHandler.make[IO](repo, gitClient)
        yield (handler, repo))
          .use { (h: ContractsHandler[IO], r: ContractsRepository[IO]) =>
            {
              for
                _         <- h.addContract(testContractV1)
                _         <- h.addContract(testContractV2)
                versionsS <- r.getAllVersionsForSubject(testSubject)
                versions  <- versionsS.compile.toList
                _         <- logger.info(s"versions before deleting: $versions")
                _ = versions shouldBe List(1, 2)
                _           <- h.updateIsMergedStatus(testSubject, testVersion2)
                contractOpt <- r.get(testSubject, testVersion2)
                _           <- logger.info(s"contract after setting isMerged: $contractOpt")
                _ = contractOpt.exists(_.isMerged) shouldBe true
                _         <- h.deleteContract(testSubject)
                versionsS <- r.getAllVersionsForSubject(testSubject)
                versions  <- versionsS.compile.toList
                _ = versions shouldBe Nil
              yield true
            }
          }
          .unsafeRunSync()
      }
    }
  }

object ContractsHandlerSpec:
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
