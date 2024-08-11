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

class ContractsServerEndpoints[F[_] : Async : MonadThrow](baseUri: String, client: ContractsRegistryHttpClient[F]) extends ContractsEndpoints:
  given createContractDTOEncoder: EntityEncoder[F, ContractDTO] = jsonEncoderOf[F, ContractDTO]
  given createContractDTODecoder: EntityDecoder[F, ContractDTO] = jsonOf[F, ContractDTO]
  
  given createContractResponseDTOEncoder: EntityEncoder[F, CreateContractResponseDTO] = jsonEncoderOf[F, CreateContractResponseDTO]
  given createContractResponseDTODecoder: EntityDecoder[F, CreateContractResponseDTO] = jsonOf[F, CreateContractResponseDTO]

  val createContractSE: ServerEndpoint[Any, F] =
    createContract.serverLogic(createContract => {
      val contract = ContractDTO(schema = createContract.schema)
      client.post(Uri.unsafeFromString(s"$baseUri/subjects/${createContract.name}/versions"), contract, None).flatMap {
        case response if response.status.code == 200 =>
          response.as[CreateContractResponseDTO].map(_.asRight[ContractErrorDTO])
        case response if response.status.code >= 400 && response.status.code < 500 =>
          BadRequestDTO(createContract.name, "FIXME").asLeft[CreateContractResponseDTO].pure[F]
      }
    })

  private val getServerEndpoints: List[ServerEndpoint[Any, F]] =
    List(createContractSE)

  val serverEndpoints = getServerEndpoints
