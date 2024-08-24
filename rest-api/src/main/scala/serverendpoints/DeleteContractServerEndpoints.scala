package io.github.sergeiionin.contractsregistrator
package serverendpoints

import client.DeleteSchemaClient
import domain.events.contracts.{ContractDeleteRequested, ContractDeleteRequestedKey}
import dto.*
import dto.errors.HttpErrorDTO
import dto.schema.*
import endpoints.DeleteContractEndpoints
import producer.EventsProducer

import cats.effect.Async
import cats.syntax.either.*
import cats.syntax.functor.*
import cats.syntax.option.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ServerEndpoint.Full

class DeleteContractServerEndpoints[F[_] : Async](
                                                   producer: EventsProducer[F, ContractDeleteRequestedKey, ContractDeleteRequested]
                                                 ) extends DeleteContractEndpoints:

  private val deleteContractVersionSE: ServerEndpoint[Any, F] =
    deleteContractVersion.serverLogic((subject, version) => {
      val key = ContractDeleteRequestedKey(subject)
      val msg = ContractDeleteRequested(subject, version.some)
      producer.produce(key, msg)
        .as(DeleteContractVersionResponseDTO(subject, version).asRight[HttpErrorDTO])
    })

  private val deleteContractSE: ServerEndpoint[Any, F] =
    deleteContract.serverLogic(subject => {
      val key = ContractDeleteRequestedKey(subject)
      val msg = ContractDeleteRequested(subject, None, deleteSubject = true)
      producer.produce(key, msg)
        .as(DeleteContractResponseDTO(subject).asRight[HttpErrorDTO])
    })

  private val getServerEndpoints: List[ServerEndpoint[Any, F]] =
    List(deleteContractVersionSE, deleteContractSE)

  val serverEndpoints = getServerEndpoints
