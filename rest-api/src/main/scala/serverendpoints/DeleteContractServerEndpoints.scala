package io.github.sergeiionin.contractsregistrator
package serverendpoints

import client.GetSchemaClient
import domain.Versions
import domain.events.contracts.*
import dto.*
import dto.errors.{HttpErrorDTO, InternalServerErrorDTO}
import dto.schema.*
import endpoints.DeleteContractEndpoints
import producer.EventsProducer

import cats.data.EitherT
import cats.syntax.applicativeError.*
import cats.effect.Async
import cats.syntax.either.*
import cats.syntax.functor.*
import cats.syntax.option.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ServerEndpoint.Full

class DeleteContractServerEndpoints[F[_]: Async](
    getClient: GetSchemaClient[F],
    producer: EventsProducer[F, ContractDeletedEventKey, ContractDeletedEvent]
) extends DeleteContractEndpoints:

  private val deleteContractVersionSE: ServerEndpoint[Any, F] =
    deleteContractVersion.serverLogic((subject, version) => {
      val k = ContractVersionDeleteRequestedKey(subject, version)
      val v = ContractVersionDeleteRequested(subject, version)
      produce[DeleteContractVersionResponseDTO](
        DeleteContractVersionResponseDTO(subject, version),
        k,
        v
      ).value
    })

  private val deleteContractSE: ServerEndpoint[Any, F] =
    deleteContract.serverLogic(subject => {
      (for
        versions <- getClient.getSchemaVersions(subject)
        k = ContractDeleteRequestedKey(subject)
        v = ContractDeleteRequested(subject, Versions.toList(versions))
        response <- produce[DeleteContractResponseDTO](DeleteContractResponseDTO(subject), k, v)
      yield response).value
    })

  private def produce[R](r: => R, k: ContractDeletedEventKey, v: ContractDeletedEvent) =
    EitherT(
      producer.produce(k, v).attempt.map {
        case Right(_) =>
          r.asRight[HttpErrorDTO]
        case Left(t) =>
          InternalServerErrorDTO(msg = s"Failed to produce delete event: ${t.getMessage}")
            .asLeft[R]
      }
    )

  private val getServerEndpoints: List[ServerEndpoint[Any, F]] =
    List(deleteContractVersionSE, deleteContractSE)

  val serverEndpoints = getServerEndpoints
