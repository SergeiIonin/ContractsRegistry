package io.github.sergeiionin.contractsregistrator
package repository

import domain.Contract
import domain.SchemaType.PROTOBUF

import cats.effect.unsafe.implicits.global
import cats.effect.{IO, Resource}
import cats.syntax.option.*
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import com.dimafeng.testcontainers.{
  Container,
  ForAllTestContainer,
  ForEachTestContainer,
  PostgreSQLContainer
}
import natchez.Trace.Implicits.noop
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.testcontainers.utility.DockerImageName
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

class ContractsRepositoryPostgresImplSpec
    extends AnyWordSpec
    with Matchers
    with TestContainerForAll
    with PostgresHelper[IO]:
  import ContractsRepositoryPostgresImplSpec.given

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

  override def beforeContainersStop(containers: Containers): Unit =
    super.beforeContainersStop(containers)
    println("Shutting down ryuk container...")
    after_All()

  "ContractsRepositoryPostgresImpl" should {
    "perform CRUD operations correctly" in {
      withContainers { container =>
        val psqlContainer: PostgreSQLContainer = container.asInstanceOf[PostgreSQLContainer]
        (for
          sessionR <- sessionPooledResource(psqlContainer)
          repo     <- ContractsRepository.make[IO](sessionR)
        yield (sessionR, repo))
          .use { (sR, repository) =>
            {
              for
                _       <- initPostgres(sR)
                _       <- repository.save(testContractV1)
                _       <- repository.save(testContractV2)
                resOpt1 <- repository.get(testSubject, 1)
                _ <- IO.whenA(resOpt1.isEmpty)(
                  IO.raiseError(new RuntimeException("Contract not found")))
                res1 = resOpt1.get
                _ = res1 shouldBe testContractV1
                versionsS <- repository.getAllVersionsForSubject(testSubject)
                versions  <- versionsS.compile.toList
                _ = versions shouldBe List(1, 2)
                _       <- repository.delete(testSubject, 2)
                resOpt2 <- repository.get(testSubject, 2)
                _ = resOpt2 shouldBe None
                _ <- repository.save(testContractV2)
                resOpt2 <- repository.get(testSubject, 2)
                subjectsS <- repository.getAllSubjects()
                subjects  <- subjectsS.compile.toList
                _ = subjects shouldBe List(testSubject)
                versionsS <- repository.getAllVersionsForSubject(testSubject)
                versions  <- versionsS.compile.toList
                _ = versions shouldBe List(1, 2)
                _ <- versionsS
                  .parEvalMapUnordered(10)(version => repository.delete(testSubject, version))
                  .compile
                  .drain
                resOpt1 <- repository.get(testSubject, 1)
                resOpt2 <- repository.get(testSubject, 2)
                _ = resOpt1 shouldBe None
                _ = resOpt2 shouldBe None
              yield true
            }
          }
          .unsafeRunSync()
      }
    }
  }

object ContractsRepositoryPostgresImplSpec:
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
