package io.github.sergeiionin.contractsregistrator
package serverendpoints

import client.{CreateSchemaClient, GetSchemaClient}
import domain.{Contract, SchemaType}
import domain.SchemaType.*
import domain.max
import domain.events.contracts.{ContractCreateRequested, ContractCreateRequestedKey}
import dto.*
import dto.schema.*
import dto.errors.{HttpErrorDTO, InternalServerErrorDTO}
import endpoints.CreateContractEndpoints
import producer.EventsProducer

import cats.data.EitherT
import cats.effect.Async
import cats.syntax.either.*
import cats.syntax.functor.*
import cats.syntax.applicativeError.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ServerEndpoint.Full

final class CreateContractServerEndpoints[F[_] : Async](
                                                         clientCreate: CreateSchemaClient[F],
                                                         clientGet: GetSchemaClient[F], 
                                                         producer: EventsProducer[F, ContractCreateRequestedKey, ContractCreateRequested]
                                                       ) extends CreateContractEndpoints:

  private def create(createContract: CreateContractDTO): EitherT[F, HttpErrorDTO, CreateContractResponseDTO] =
    val subject = createContract.subject
    val schema = createContract.schema
    val schemaType = createContract.schemaType
    val createSchemaDTO = CreateSchemaDTO(schema = schema)
    for
      response <- clientCreate.createSchema(createContract.subject, createSchemaDTO)
      versions <- clientGet.getSchemaVersions(createContract.subject)
      version  = versions.max
      id       = response.id
      contract = Contract(subject, version, id, schema, SchemaType.fromString(schemaType), false, None)
      _        <- EitherT(
                    producer.produce(
                      ContractCreateRequestedKey(subject, version),
                      ContractCreateRequested(contract)
                    ).attempt.map {
                      case Right(_) =>
                        ().asRight[HttpErrorDTO]
                      case Left(t) =>
                        InternalServerErrorDTO(msg = s"Failed to produce create event: ${t.getMessage}")
                          .asLeft[Unit]
                    }
                  )
    yield CreateContractResponseDTO(subject, id)

  private val createContractSE: ServerEndpoint[Any, F] =
    createContract.serverLogic(createContract => {
      create(createContract).value
    })
  
  private val getServerEndpoints: List[ServerEndpoint[Any, F]] =
    List(createContractSE)

  val serverEndpoints = getServerEndpoints
