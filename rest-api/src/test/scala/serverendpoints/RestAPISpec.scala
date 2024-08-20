package io.github.sergeiionin.contractsregistrator
package serverendpoints

import cats.effect.IO
import cats.syntax.option.*
import cats.effect.unsafe.implicits.global
import cats.effect.testing.specs2.CatsEffect
import sttp.tapir
import sttp.model.StatusCode.{BadRequest, Ok, Unauthorized}
import sttp.tapir.Schema
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ServerEndpoint.Full
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.integ.cats.effect.CatsMonadError
import sttp.client3.{Response, UriContext, basicRequest}
import sttp.client3.testing.SttpBackendStub
import org.specs2.mutable.Specification
import org.specs2.matcher.ShouldMatchers
import org.specs2.mutable.*
import org.specs2.specification.core.SpecStructure
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import config.RestApiApplicationConfig
import http.client.ContractsRegistryHttpClientTestImpl
import dto.{ContractErrorDTO, ContractDTO, CreateContractDTO, CreateContractResponseDTO, DeleteContractResponseDTO, DeleteContractVersionResponseDTO}
import endpoints.ContractEndpoint
import org.http4s.Uri
import org.scalatest.Ignore

class RestAPISpec extends Specification with CatsEffect with ContractsHelper:
  import RestAPISpec.*
  import ContractsServerEndpoints.given
  import ContractHelper.given
  import dto.schemaregistry.SchemaRegistryDTO.given
  import endpoints.ContractsEndpoints.*

  import io.circe.generic.semiauto
  import io.circe.Encoder

  override def is: SpecStructure = sequential ^ super.is

  val createContractDTOEncoder = summon[Encoder[CreateContractDTO]]

  def addContracts(subject: String): IO[Unit] =
    for
      _ <- contractsClient.post(Uri.unsafeFromString(s"$baseClientUri/subjects/$subject/versions"), schemaDTOv1, None)
      _ <- contractsClient.post(Uri.unsafeFromString(s"$baseClientUri/subjects/$subject/versions"), schemaDTOv2, None)
    yield ()

  def deleteContractsForSubject(subject: String): IO[Unit] =
    contractsClient.delete(Uri.unsafeFromString(s"$baseClientUri/subjects/$subject"), None).void

  "createContract" should {
    val backendCreateContractStub =
      TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
        .whenServerEndpoint(createContractServerEndpoint)
        .thenRunLogic()
        .backend()

    val subject = "foo"

    "return 200 when a contract was created" in {
      def responseIO = 
        basicRequest
        .post(uri"http://test.com/$contracts")
        .body(createContractDTOJson(subject, schemaV1))
        .send(backendCreateContractStub)

      for
        response <- responseIO
        _        <- IO.println(response)
        _        <- deleteContractsForSubject(subject)
        _        = response.code must beEqualTo(Ok)
      yield true
    }
    
    "return 400 when a contract payload is incorrect" in {
      val wrongJson = 
        "{\"title\": \"bad_contract\"}"

      def responseIO =
        basicRequest
          .post(uri"http://test.com/$contracts")
          .body(wrongJson)
          .send(backendCreateContractStub)

      for
        response <- responseIO
        _ <- IO.println(response)
        _ = response.code must beEqualTo(BadRequest)
      yield true
    }
  }
  
  "getContractVersion" should {
    val backendGetContractVersionStub =
      TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
        .whenServerEndpoint(getContractVersionServerEndpoint)
        .thenRunLogic()
        .backend()

    val subject = "foo"

    "return 200 when a contract version was retrieved" in {
      def getResponseIO(version: Int) =
          basicRequest
            .get(uri"http://test.com/$contracts/$subject/$versions/$version")
            .send(backendGetContractVersionStub)

      for
        _ <- addContracts(subject)
        response_1 <- getResponseIO(1)
        _ <- IO.println(response_1)
        response_2 <- getResponseIO(2)
        _ <- IO.println(response_2)
        _ <- deleteContractsForSubject(subject)
        _ = response_1.code must beEqualTo(Ok)
        _ = response_2.code must beEqualTo(Ok)
      yield true
    }
  } 

  "deleteContractVersion" should {
    val backendDeleteContractVersionStub =
      TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
        .whenServerEndpoint(deleteContractVersionServerEndpoint)
        .thenRunLogic()
        .backend()

    val subject = "foo"
    val version = 1

    "return 200 when a contract version was deleted" in {
      def deleteResponseIO =
          basicRequest
            .delete(uri"http://test.com/$contracts/$subject/$versions/$version")
            .send(backendDeleteContractVersionStub)

      for
          _ <- addContracts(subject)
          response <- deleteResponseIO
          _ <- IO.println(response)
          _ <- deleteContractsForSubject(subject)
          _ = response.code must beEqualTo(Ok)
      yield true
    }
  }

  "deleteContractSubject" should {
    val backendDeleteContractSubjectStub =
      TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
        .whenServerEndpoint(deleteContractSubjectServerEndpoint)
        .thenRunLogic()
        .backend()

    val subject = "foo"

    "return 200 when a contract subject was deleted" in {
      def deleteResponseIO =
          basicRequest
            .delete(uri"http://test.com/$contracts/$subject/")
            .send(backendDeleteContractSubjectStub)

      for
          _ <- addContracts(subject)
          response <- deleteResponseIO
          _ <- IO.println(response)
          _ = response.code must beEqualTo(Ok)
      yield true
    }
  }

object RestAPISpec:
  val config = RestApiApplicationConfig.load
  val host = config.restApi.host
  val port = config.restApi.port
  val baseClientUri = s"${config.schemaRegistry.host}:${config.schemaRegistry.port}"
  
  val contractsClient = ContractsRegistryHttpClientTestImpl.make[IO]()

  val commandsServerEndpoints = ContractsServerEndpoints[IO](baseClientUri, contractsClient)

  val nameToServerEndpoint = commandsServerEndpoints.serverEndpoints.map(se => se.info.name.get -> se).toMap

  val createContractServerEndpoint = nameToServerEndpoint(ContractEndpoint.CreateContract.toString)
    .asInstanceOf[Full[Unit, Unit, CreateContractDTO, ContractErrorDTO, CreateContractResponseDTO, Any, IO]]
  
  val deleteContractVersionServerEndpoint = nameToServerEndpoint(ContractEndpoint.DeleteContractVersion.toString)
    .asInstanceOf[Full[Unit, Unit, (String, Int), ContractErrorDTO, DeleteContractVersionResponseDTO, Any, IO]]
    
  val deleteContractSubjectServerEndpoint = nameToServerEndpoint(ContractEndpoint.DeleteContractSubject.toString)
    .asInstanceOf[Full[Unit, Unit, String, ContractErrorDTO, DeleteContractResponseDTO, Any, IO]]

  val getContractVersionServerEndpoint = nameToServerEndpoint(ContractEndpoint.GetContractVersion.toString)
    .asInstanceOf[Full[Unit, Unit, (String, Int), ContractErrorDTO, ContractDTO, Any, IO]]

  // todo
  // implement other get endpoints
  
