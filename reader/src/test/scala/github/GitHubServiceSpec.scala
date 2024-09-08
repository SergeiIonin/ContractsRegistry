package io.github.sergeiionin.contractsregistrator
package github

import domain.Contract
import domain.SchemaType.PROTOBUF
import github.{GitHubClient, GitHubClientTest, GitHubClientTestImpl}

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.testcontainers.utility.DockerImageName
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}

class GitHubServiceSpec
    extends AnyWordSpec
    with Matchers:
  import GitHubServiceSpec.given

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
  
  "GitHubService" should {
    "add and delete contracts" in {
        (for
          gitClient <- GitHubClientTest.test[IO]()
          service   <- GitHubService.make[IO](gitClient)
        yield service)
          .use { (s: GitHubService[IO]) =>
            {
              for
                _         <- s.addContract(testContractV1)
                _         <- s.addContract(testContractV2)
                _         <- s.deleteContract(testSubject, List(1,2))
              yield true
            }
          }
          .unsafeRunSync()
    }
  }

object GitHubServiceSpec:
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
