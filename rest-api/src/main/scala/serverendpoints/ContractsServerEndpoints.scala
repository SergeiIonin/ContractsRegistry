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
import dto.schemaregistry.*

class ContractsServerEndpoints[F[_] : Async : MonadThrow](baseUri: String, client: ContractsRegistryHttpClient[F]) extends ContractsEndpoints:
  import ContractsServerEndpoints.given
  import dto.schemaregistry.SchemaRegistryDTO.given

  // fixme add BadRequestDTO message
  private val createContractSE: ServerEndpoint[Any, F] =
    createContract.serverLogic(createContract => {
      val schema = SchemaDTO(schema = createContract.schema)
      client
        .post(Uri.unsafeFromString(s"$baseUri/subjects/${createContract.subject}/versions"), schema, None)
        .flatMap {
          case response if response.status.code == 200 =>
            response
              .as[CreateSchemaResponseDTO]
              .map(r =>
                CreateContractResponseDTO(createContract.subject, r.id)
                  .asRight[ContractErrorDTO]
              )
          case response if response.status.code >= 400 && response.status.code < 500 =>
            BadRequestDTO(createContract.subject, "FIXME") // fixme
              .asLeft[CreateContractResponseDTO]
              .pure[F]
      }
    })
  
  private val getContractVersionSE: ServerEndpoint[Any, F] =
    getContractVersion.serverLogic((subject, version) => {
      val uri = s"$baseUri/subjects/$subject/versions/$version"
      client.get(Uri.unsafeFromString(uri), None).flatMap {
        case response if response.status.code == 200 =>
          response
            .as[ContractDTO]
            .map(_.asRight[ContractErrorDTO])
        case response if response.status.code >= 400 && response.status.code < 500 =>
          BadRequestDTO(subject, "FIXME")
            .asLeft[ContractDTO]
            .pure[F]
      }
    })
  
  private val getVersionsSE: ServerEndpoint[Any, F] =
    getVersions.serverLogic(subject => {
      val uri = s"$baseUri/subjects/$subject/versions"
      client.get(Uri.unsafeFromString(uri), None).flatMap {
        case response if response.status.code == 200 =>
          response
            .as[List[Int]]
            .map(_.asRight[ContractErrorDTO])
        case response if response.status.code >= 400 && response.status.code < 500 =>
          BadRequestDTO(subject, "FIXME")
            .asLeft[List[Int]]
            .pure[F]
      }
    })
  
  private val getSubjectsSE: ServerEndpoint[Any, F] =
    getSubjects.serverLogic(_ => {
      val uri = s"$baseUri/subjects"
      client.get(Uri.unsafeFromString(uri), None).flatMap {
        case response if response.status.code == 200 =>
          response
            .as[List[String]]
            .map(_.asRight[ContractErrorDTO])
        case response if response.status.code >= 400 && response.status.code < 500 =>
          BadRequestDTO("FIXME", "FIXME")
            .asLeft[List[String]]
            .pure[F]
      }
    })
  
  private val deleteContractVersionSE: ServerEndpoint[Any, F] =
    deleteContractVersion.serverLogic((subject, version) => {
      val uri = s"$baseUri/subjects/$subject/versions/$version"
      client
        .delete(Uri.unsafeFromString(uri), None)
        .flatMap {
          case response if response.status.code == 200 =>
            response.as[Int]
              .map(version => 
                DeleteContractVersionResponseDTO(subject, version)
                  .asRight[ContractErrorDTO]
              )
          case response if response.status.code >= 400 && response.status.code < 500 =>
            BadRequestDTO(subject, "FIXME")
              .asLeft[DeleteContractVersionResponseDTO]
              .pure[F]
        }
    })
  
  private val deleteContractSE: ServerEndpoint[Any, F] =
    deleteContract.serverLogic(subject => {
      val uri = s"$baseUri/subjects/$subject"
      client.delete(Uri.unsafeFromString(uri), None).flatMap {
        case response if response.status.code == 200 =>
          response
            .as[List[Int]]
            .map(versions =>
              DeleteContractResponseDTO(subject, versions)
                .asRight[ContractErrorDTO]
            )
        case response if response.status.code >= 400 && response.status.code < 500 =>
          BadRequestDTO(subject, "FIXME")
            .asLeft[DeleteContractResponseDTO]
            .pure[F]
      }
    })
  
  private val getServerEndpoints: List[ServerEndpoint[Any, F]] =
    List(createContractSE, getContractVersionSE, getVersionsSE, getSubjectsSE, deleteContractVersionSE, deleteContractSE)

  val serverEndpoints = getServerEndpoints

object ContractsServerEndpoints:
  given contractDtoEncoder[F[_]]: EntityEncoder[F, ContractDTO] = jsonEncoderOf[F, ContractDTO]
  given contractDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, ContractDTO] = jsonOf[F, ContractDTO]

  given createContractResponseDtoEncoder[F[_]]: EntityEncoder[F, CreateContractResponseDTO] = jsonEncoderOf[F, CreateContractResponseDTO]

  given createSchemaResponseDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, CreateSchemaResponseDTO] = jsonOf[F, CreateSchemaResponseDTO]

  given versionDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, Int] = jsonOf[F, Int]
  
  given versionsDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, List[Int]] = jsonOf[F, List[Int]]
  
  given subjectsDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, List[String]] = jsonOf[F, List[String]]
  
  given deleteContractVersionResponseDtoEncoder[F[_]]: EntityEncoder[F, DeleteContractVersionResponseDTO] = jsonEncoderOf[F, DeleteContractVersionResponseDTO]

  given deleteContractResponseDtoEncoder[F[_]]: EntityEncoder[F, DeleteContractResponseDTO] = jsonEncoderOf[F, DeleteContractResponseDTO]
