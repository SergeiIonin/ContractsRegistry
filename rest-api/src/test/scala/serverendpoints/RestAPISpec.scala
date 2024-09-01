package io.github.sergeiionin.contractsregistrator
package serverendpoints

import client.DeleteSchemaClient
import client.schemaregistry.{
  CreateSchemaClientImpl,
  DeleteSchemaClientImpl,
  GetSchemaClientImpl
}
import config.RestApiApplicationConfig
import domain.events.contracts.{
  ContractCreateRequestedKey,
  ContractCreateRequested,
  ContractDeletedEvent,
  ContractDeletedEventKey
}
import domain.{Contract, SchemaType}
import dto.*
import dto.errors.HttpErrorDTO
import endpoints.{RootContractsEndpoint, ContractEndpoint}
import http.client.HttpClientTestImpl
import producer.{EventsProducer, TestContractsEventsProducer}
import service.ContractServiceTestImpl

import cats.effect.IO
import cats.effect.testing.specs2.CatsEffect
import cats.effect.unsafe.implicits.global
import cats.syntax.option.*
import org.http4s.Uri
import org.scalatest.Ignore
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.specs2.matcher.ShouldMatchers
import org.specs2.mutable.*
import org.specs2.specification.core.SpecStructure
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{Response, UriContext, basicRequest}
import sttp.model.StatusCode.{BadRequest, Ok, Unauthorized}
import sttp.tapir
import sttp.tapir.Schema
import sttp.tapir.integ.cats.effect.CatsMonadError
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ServerEndpoint.Full
import sttp.tapir.server.stub.TapirStubInterpreter

class RestAPISpec
    extends Specification
    with CatsEffect
    with SchemasHelper
    with RootContractsEndpoint:
  import endpoints.ContractEndpoint.*
  import http4s.entitycodecs.CreateSchemaDtoEntityCodec.given
  import http4s.entitycodecs.CreateSchemaResponseDtoEntityCodec.given

  import RestAPISpec.*
  import SchemasHelper.given
  import io.circe.Encoder
  import io.circe.generic.semiauto

  override def is: SpecStructure = sequential ^ super.is

  val createContractDTOEncoder = summon[Encoder[CreateContractDTO]]

  def addContracts(subject: String): IO[Unit] =
    (for
      _ <- createSchemaClient.createSchema(subject, schemaDTOv1)
      _ <- contractService.saveContract(
        Contract(subject, 1, 1, schemaV1, SchemaType.PROTOBUF, true, false.some))
      _ <- createSchemaClient.createSchema(subject, schemaDTOv2)
      _ <- contractService.saveContract(
        Contract(subject, 2, 2, schemaV2, SchemaType.PROTOBUF, true, false.some))
    yield ()).value.void

  def deleteContractsForSubject(subject: String): IO[Unit] =
    (for
      _ <- deleteSchemaClient.deleteSchemaSubject(subject)
      _ <- contractService.deleteContract(subject)
    yield ()).value.void

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
        _ = response.code must beEqualTo(Ok)
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
        _        <- IO.println(response)
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
        _          <- addContracts(subject)
        response_1 <- getResponseIO(1)
        _          <- IO.println(response_1)
        response_2 <- getResponseIO(2)
        _          <- IO.println(response_2)
        _          <- deleteContractsForSubject(subject)
        _ = response_1.code must beEqualTo(Ok)
        _ = response_2.code must beEqualTo(Ok)
      yield true
    }
    // todo add 400 case
  }

  "getVersions" should {
    val backendGetVersionsStub =
      TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
        .whenServerEndpoint(getVersionsServerEndpoint)
        .thenRunLogic()
        .backend()

    val subject = "foo"

    "return 200 when a contract versions were retrieved" in {
      def getResponseIO(subject: String) =
        basicRequest
          .get(uri"http://test.com/$contracts/$subject/$versions")
          .send(backendGetVersionsStub)

      for
        _        <- addContracts(subject)
        response <- getResponseIO(subject)
        _        <- IO.println(response)
        _        <- deleteContractsForSubject(subject)
        _ = response.code must beEqualTo(Ok)
      yield true
    }

    "return 400 when a subject wasn't found" in {
      def getResponseIO(subject: String) =
        basicRequest
          .get(uri"http://test.com/$contracts/$subject/$versions")
          .send(backendGetVersionsStub)

      for
        _        <- addContracts("foo")
        response <- getResponseIO("bar")
        _        <- IO.println(response)
        _        <- deleteContractsForSubject("foo")
        _ = response.code must beEqualTo(BadRequest)
      yield true
    }
  }

  "getSubjects" should {
    val backendGetSubjectsStub =
      TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
        .whenServerEndpoint(getSubjectsServerEndpoint)
        .thenRunLogic()
        .backend()

    "return 200 when a contract subjects were retrieved" in {
      def getResponseIO =
        basicRequest.get(uri"http://test.com/$contracts/$subjects").send(backendGetSubjectsStub)

      for
        _        <- addContracts("foo")
        _        <- addContracts("bar")
        response <- getResponseIO
        _        <- IO.println(response)
        _        <- deleteContractsForSubject("foo")
        _        <- deleteContractsForSubject("bar")
        _ = response.code must beEqualTo(Ok)
      yield true
    }
  }

  "getLatestContract" should {
    val backendGetLatestContractStub =
      TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
        .whenServerEndpoint(getLatestContractServerEndpoint)
        .thenRunLogic()
        .backend()

    val subject = "foo"

    "return 200 when a latest contract version was retrieved" in {
      def getResponseIO(subject: String) =
        basicRequest
          .get(uri"http://test.com/$contracts/$subject/$latest")
          .send(backendGetLatestContractStub)

      for
        _        <- addContracts(subject)
        response <- getResponseIO(subject)
        _        <- IO.println(response)
        _        <- deleteContractsForSubject(subject)
        _ = response.code must beEqualTo(Ok)
      yield true
    }

    "return 400 when a subject wasn't found" in {
      def getResponseIO(subject: String) =
        basicRequest
          .get(uri"http://test.com/$contracts/$subject/$latest")
          .send(backendGetLatestContractStub)

      for
        _        <- addContracts("foo")
        response <- getResponseIO("bar")
        _        <- IO.println(response)
        _        <- deleteContractsForSubject("foo")
        _ = response.code must beEqualTo(BadRequest)
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
        _        <- addContracts(subject)
        response <- deleteResponseIO
        _        <- IO.println(response)
        _        <- deleteContractsForSubject(subject)
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
        _        <- addContracts(subject)
        response <- deleteResponseIO
        _        <- IO.println(response)
        _ = response.code must beEqualTo(Ok)
      yield true
    }
  }

object RestAPISpec:
  val config = RestApiApplicationConfig.load
  val host = config.restApi.host
  val port = config.restApi.port
  val baseClientUri = s"${config.schemaRegistry.host}:${config.schemaRegistry.port}"

  val httpClient = HttpClientTestImpl.make[IO]()
  val schemasClient = CreateSchemaClientImpl(baseClientUri, httpClient)

  val getSchemaClient = GetSchemaClientImpl[IO](baseClientUri, httpClient)
  val createSchemaClient = CreateSchemaClientImpl[IO](baseClientUri, httpClient)
  val deleteSchemaClient = DeleteSchemaClientImpl[IO](baseClientUri, httpClient)

  val contractService = ContractServiceTestImpl[IO]()
  val contractsDeleteEventsProducer
      : EventsProducer[IO, ContractDeletedEventKey, ContractDeletedEvent] =
    new TestContractsEventsProducer[IO, ContractDeletedEventKey, ContractDeletedEvent]() {}
  val contractsCreateEventsProducer
      : EventsProducer[IO, ContractCreateRequestedKey, ContractCreateRequested] =
    new TestContractsEventsProducer[
      IO,
      ContractCreateRequestedKey,
      ContractCreateRequested]() {}

  val getContractServerEndpoints = GetContractServerEndpoints[IO](contractService)
  val createContractServerEndpoints = CreateContractServerEndpoints[IO](
    createSchemaClient,
    getSchemaClient,
    contractsCreateEventsProducer)
  val deleteContractServerEndpoints =
    DeleteContractServerEndpoints[IO](getSchemaClient, contractsDeleteEventsProducer)

  val nameToServerEndpoint = (createContractServerEndpoints.serverEndpoints ++
    getContractServerEndpoints.serverEndpoints ++
    deleteContractServerEndpoints.serverEndpoints).map(se => se.info.name.get -> se).toMap

  val createContractServerEndpoint = nameToServerEndpoint(
    ContractEndpoint.CreateContract.toString).asInstanceOf[Full[
    Unit,
    Unit,
    CreateContractDTO,
    HttpErrorDTO,
    CreateContractResponseDTO,
    Any,
    IO]]

  val deleteContractVersionServerEndpoint = nameToServerEndpoint(
    ContractEndpoint.DeleteContractVersion.toString).asInstanceOf[Full[
    Unit,
    Unit,
    (String, Int),
    HttpErrorDTO,
    DeleteContractVersionResponseDTO,
    Any,
    IO]]

  val deleteContractSubjectServerEndpoint = nameToServerEndpoint(
    ContractEndpoint.DeleteContractSubject.toString)
    .asInstanceOf[Full[Unit, Unit, String, HttpErrorDTO, DeleteContractResponseDTO, Any, IO]]

  val getContractVersionServerEndpoint = nameToServerEndpoint(
    ContractEndpoint.GetContractVersion.toString)
    .asInstanceOf[Full[Unit, Unit, (String, Int), HttpErrorDTO, ContractDTO, Any, IO]]

  val getVersionsServerEndpoint = nameToServerEndpoint(ContractEndpoint.GetVersions.toString)
    .asInstanceOf[Full[Unit, Unit, String, HttpErrorDTO, List[Int], Any, IO]]

  val getSubjectsServerEndpoint = nameToServerEndpoint(ContractEndpoint.GetSubjects.toString)
    .asInstanceOf[Full[Unit, Unit, Unit, HttpErrorDTO, List[String], Any, IO]]

  val getLatestContractServerEndpoint = nameToServerEndpoint(
    ContractEndpoint.GetLatestContract.toString)
    .asInstanceOf[Full[Unit, Unit, String, HttpErrorDTO, ContractDTO, Any, IO]]
