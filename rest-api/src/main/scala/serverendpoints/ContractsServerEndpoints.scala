package io.github.sergeiionin.contractsregistrator
package serverendpoints

import org.http4s.{EntityDecoder, EntityEncoder}
import cats.Monad
import cats.syntax.all.*
import cats.MonadThrow
import cats.effect.kernel.Async
import sttp.tapir.server.ServerEndpoint
import io.github.sergeiionin.contractsregistrator.dto.*
import io.github.sergeiionin.contractsregistrator.endpoints.ContractsEndpoints
import io.github.sergeiionin.contractsregistrator.http.client.ContractsRegistryHttpClient
import io.github.sergeiionin.contractsregistrator.repository.ContractsRepository
import sttp.tapir.server.ServerEndpoint.Full
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.Uri
import io.github.sergeiionin.contractsregistrator.dto.schemaregistry.DTO.*

class ContractsServerEndpoints[F[_] : Async : MonadThrow](baseUri: String, client: ContractsRegistryHttpClient[F]) extends ContractsEndpoints:
  given createContractDtoEncoder: EntityEncoder[F, ContractDTO] = jsonEncoderOf[F, ContractDTO]
  given createContractDtoDecoder: EntityDecoder[F, ContractDTO] = jsonOf[F, ContractDTO]
  
  given createContractResponseDtoEncoder: EntityEncoder[F, CreateContractResponseDTO] = jsonEncoderOf[F, CreateContractResponseDTO]
  given createContractResponseDtoDecoder: EntityDecoder[F, CreateContractResponseDTO] = jsonOf[F, CreateContractResponseDTO]
  
  given createSchemaResponseDtoEncoder: EntityEncoder[F, CreateSchemaResponseDTO] = jsonEncoderOf[F, CreateSchemaResponseDTO]
  given createSchemaResponseDtoDecoder: EntityDecoder[F, CreateSchemaResponseDTO] = jsonOf[F, CreateSchemaResponseDTO]

  val createContractSE: ServerEndpoint[Any, F] =
    createContract.serverLogic(createContract => {
      val contract = ContractDTO(schema = createContract.schema)
      client.post(Uri.unsafeFromString(s"$baseUri/subjects/${createContract.name}/versions"), contract, None).flatMap {
        case response if response.status.code == 200 =>
          response.as[CreateSchemaResponseDTO].map(r => CreateContractResponseDTO(createContract.name, r.id).asRight[ContractErrorDTO])
        case response if response.status.code >= 400 && response.status.code < 500 =>
          BadRequestDTO(createContract.name, "FIXME").asLeft[CreateContractResponseDTO].pure[F]
      }
    })

  private val getServerEndpoints: List[ServerEndpoint[Any, F]] =
    List(createContractSE)

  val serverEndpoints = getServerEndpoints
