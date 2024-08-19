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
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import config.RestApiApplicationConfig
import http.client.ContractsRegistryHttpClientTestImpl
import dto.{ContractDTO, ContractErrorDTO, CreateContractDTO, CreateContractResponseDTO, DeleteContractResponseDTO, DeleteContractVersionResponseDTO}

class RestAPISpec extends Specification with CatsEffect:
  import RestAPISpec.*

  import io.circe.generic.semiauto
  import io.circe.Encoder
  
  val CreateContractDTOEncoder = summon[Encoder[CreateContractDTO]]

  "createContract" should {
    val backendCreateContractStub =
      TapirStubInterpreter(SttpBackendStub(new CatsMonadError[IO]()))
        .whenServerEndpoint(createContractServerEndpoint)
        .thenRunLogic()
        .backend()

    "return 200 when a contract was created" in {
      val schema =
        """
          | syntax = \"proto3\";\n
          | package Foo;\n\
          |  message Bar {\n
          |  string a = 1;\n
          |  int32 b = 2;\n
          |  int32 c = 3;\n
          |  string f = 4;\n
          |  }\n
          |""".stripMargin
      val createContractDTO =
        CreateContractDTO(name = "new_contract", schema = schema)
      val createContractDTOJson = CreateContractDTOEncoder.apply(createContractDTO).toString

      def responseIO = 
        basicRequest
        .post(uri"http://test.com/contracts")
        .body(createContractDTOJson)
        .send(backendCreateContractStub)

      for
        response <- responseIO
        _        <- IO.println(response)
        _        = response.code must beEqualTo(Ok)
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

  val createContractServerEndpoint = nameToServerEndpoint("CreateContract")
    .asInstanceOf[Full[Unit, Unit, CreateContractDTO, ContractErrorDTO, CreateContractResponseDTO, Any, IO]]
  
  val deleteContractVersionServerEndpoint = nameToServerEndpoint("DeleteContractVersion")
    .asInstanceOf[Full[Unit, Unit, (String, Int), ContractErrorDTO, DeleteContractVersionResponseDTO, Any, IO]]
    
  val deleteContractServerEndpoint = nameToServerEndpoint("DeleteContract")
    .asInstanceOf[Full[Unit, Unit, String, ContractErrorDTO, DeleteContractResponseDTO, Any, IO]]

  /*val getContractServerEndpoint = nameToServerEndpoint("GetContract")
    .asInstanceOf[Full[Unit, Unit, (String, Int), ContractErrorDTO, ContractDTO, Any, IO]]
  
  val getContractsServerEndpoint = nameToServerEndpoint("GetContracts")
    .asInstanceOf[Full[Unit, Unit, Unit, ContractErrorDTO, List[ContractDTO], Any, IO]]
*/