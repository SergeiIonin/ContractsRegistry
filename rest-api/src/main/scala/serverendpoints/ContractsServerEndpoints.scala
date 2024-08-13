package io.github.sergeiionin.contractsregistrator
package serverendpoints

import org.http4s.{EntityDecoder, EntityEncoder}
import cats.Monad
import cats.effect.Concurrent
import cats.syntax.all.*
import cats.MonadThrow
import cats.effect.kernel.Async
import sttp.tapir.server.ServerEndpoint
import dto.*
import endpoints.ContractsEndpoints
import http.client.ContractsRegistryHttpClient
import repository.ContractsRepository
import sttp.tapir.server.ServerEndpoint.Full
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.Uri
import dto.schemaregistry.DTO.*

class ContractsServerEndpoints[F[_] : Async : MonadThrow](baseUri: String, client: ContractsRegistryHttpClient[F]) extends ContractsEndpoints:
  import ContractsServerEndpoints.given
  
  // fixme add BadRequestDTO message
  private val createContractSE: ServerEndpoint[Any, F] =
    createContract.serverLogic(createContract => {
      val contract = ContractDTO(schema = createContract.schema)
      client
        .post(Uri.unsafeFromString(s"$baseUri/subjects/${createContract.name}/versions"), contract, None)
        .flatMap {
          case response if response.status.code == 200 =>
            response
              .as[CreateSchemaResponseDTO]
              .map(r =>
                CreateContractResponseDTO(createContract.name, r.id)
                  .asRight[ContractErrorDTO]
              )
          case response if response.status.code >= 400 && response.status.code < 500 =>
            BadRequestDTO(createContract.name, "FIXME")
              .asLeft[CreateContractResponseDTO]
              .pure[F]
      }
    })

  private val deleteContractVersionSE: ServerEndpoint[Any, F] =
    deleteContractVersion.serverLogic((name, version) => {
      val uri = s"$baseUri/subjects/$name/versions/$version"
      client
        .delete(Uri.unsafeFromString(uri), None)
        .flatMap {
          case response if response.status.code == 200 =>
            response
              .as[Int]
              .map(version => 
                DeleteContractVersionResponseDTO(name, version)
                  .asRight[ContractErrorDTO]
              )
          case response if response.status.code >= 400 && response.status.code < 500 =>
            BadRequestDTO(name, "FIXME")
              .asLeft[DeleteContractVersionResponseDTO]
              .pure[F]
      }
    })
  
  private val deleteContractSE: ServerEndpoint[Any, F] =
    deleteContract.serverLogic(name => {
      val uri = s"$baseUri/subjects/$name"
      client.delete(Uri.unsafeFromString(uri), None).flatMap {
        case response if response.status.code == 200 =>
          response
            .as[List[Int]]
            .map(versions =>
              DeleteContractResponseDTO(name, versions)
                .asRight[ContractErrorDTO]
            )
        case response if response.status.code >= 400 && response.status.code < 500 =>
          BadRequestDTO(name, "FIXME")
            .asLeft[DeleteContractResponseDTO]
            .pure[F]
      }
    })
  
  private val getServerEndpoints: List[ServerEndpoint[Any, F]] =
    List(createContractSE, deleteContractVersionSE, deleteContractSE)

  val serverEndpoints = getServerEndpoints

object ContractsServerEndpoints:
  given createContractDtoEncoder[F[_] : Concurrent]: EntityEncoder[F, ContractDTO] = jsonEncoderOf[F, ContractDTO]
  given createContractDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, ContractDTO] = jsonOf[F, ContractDTO]

  given createContractResponseDtoEncoder[F[_] : Concurrent]: EntityEncoder[F, CreateContractResponseDTO] = jsonEncoderOf[F, CreateContractResponseDTO]
  //given createContractResponseDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, CreateContractResponseDTO] = jsonOf[F, CreateContractResponseDTO]

  //given createSchemaResponseDtoEncoder[F[_] : Concurrent]: EntityEncoder[F, CreateSchemaResponseDTO] = jsonEncoderOf[F, CreateSchemaResponseDTO]
  given createSchemaResponseDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, CreateSchemaResponseDTO] = jsonOf[F, CreateSchemaResponseDTO]

  //given deleteSchemaVersionDtoEncoder[F[_] : Concurrent]: EntityEncoder[F, DeleteSchemaVersionResponseDTO] = jsonEncoderOf[F, DeleteSchemaVersionResponseDTO]
  //given deleteSchemaVersionDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, DeleteSchemaVersionResponseDTO] = jsonOf[F, DeleteSchemaVersionResponseDTO]
  given deleteSchemaVersionDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, Int] = jsonOf[F, Int]
  
  //given deleteDeleteSchemaResponseDtoEncoder[F[_] : Concurrent]: EntityEncoder[F, DeleteSchemaResponseDTO] = jsonEncoderOf[F, DeleteSchemaResponseDTO]
  //given deleteDeleteSchemaResponseDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, DeleteSchemaResponseDTO] = jsonOf[F, DeleteSchemaResponseDTO]
  given deleteDeleteSchemaResponseDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, List[Int]] = jsonOf[F, List[Int]]
  
  given deleteContractVersionResponseDtoEncoder[F[_] : Concurrent]: EntityEncoder[F, DeleteContractVersionResponseDTO] = jsonEncoderOf[F, DeleteContractVersionResponseDTO]
  //given deleteContractVersionResponseDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, DeleteContractVersionResponseDTO] = jsonOf[F, DeleteContractVersionResponseDTO]

  given deleteContractResponseDtoEncoder[F[_] : Concurrent]: EntityEncoder[F, DeleteContractResponseDTO] = jsonEncoderOf[F, DeleteContractResponseDTO]
  //given deleteContractResponseDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, DeleteContractResponseDTO] = jsonOf[F, DeleteContractResponseDTO]
