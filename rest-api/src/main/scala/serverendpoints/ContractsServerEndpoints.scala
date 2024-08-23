package io.github.sergeiionin.contractsregistrator
package serverendpoints

import org.http4s.{EntityDecoder, EntityEncoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.Uri
import cats.Monad
import cats.effect.Concurrent
import cats.syntax.all.*
import cats.MonadThrow
import cats.effect.kernel.Async
import sttp.tapir.server.ServerEndpoint
import dto.*
import endpoints.ContractsEndpoints
import client.SchemasClient
import repository.ContractsRepository

import sttp.tapir.server.ServerEndpoint.Full
import dto.schema.{CreateSchemaDTO, *}

class ContractsServerEndpoints[F[_] : Async : MonadThrow](client: SchemasClient[F]) extends ContractsEndpoints:
  import http4s.entitycodecs.CreateSchemaDtoEntityCodec.given
  import http4s.entitycodecs.SchemaDtoEntityCodec.given
  import http4s.entitycodecs.CreateSchemaResponseDtoEntityCodec.given

  // fixme add BadRequestDTO message
  private val createContractSE: ServerEndpoint[Any, F] =
    createContract.serverLogic(createContract => {
      val createSchemaDTO = CreateSchemaDTO(schema = createContract.schema)
      client
        .createSchema(createContract.subject, createSchemaDTO)
        .map(response => CreateContractResponseDTO(createContract.subject, response.id))
        .value
    })
  
  private val getContractVersionSE: ServerEndpoint[Any, F] =
    getContractVersion.serverLogic((subject, version) => {
      client
        .getSchemaVersion(subject, version)
        .map(ContractDTO.fromSchemaDTO)
        .value
    })
  
  private val getVersionsSE: ServerEndpoint[Any, F] =
    getVersions.serverLogic(subject => {
      client
        .getSchemaVersions(subject)
        .map(Versions.toList)
        .value
    })
  
  private val getSubjectsSE: ServerEndpoint[Any, F] =
    getSubjects.serverLogic(_ => {
      client
        .getSubjects()
        .map(Subjects.toList)
        .value
    })
  
  private val getLatestContractSE: ServerEndpoint[Any, F] =
    getLatestContract.serverLogic(subject => {
      client
        .getLatestSchema(subject)
        .map(ContractDTO.fromSchemaDTO)
        .value
    })
  
  private val deleteContractVersionSE: ServerEndpoint[Any, F] =
    deleteContractVersion.serverLogic((subject, version) => {
      client
        .deleteSchemaVersion(subject, version)
        .map(_ => DeleteContractVersionResponseDTO(subject, version))
        .value
    })
  
  private val deleteContractSE: ServerEndpoint[Any, F] =
    deleteContract.serverLogic(subject => {
      client
        .deleteSchemaSubject(subject)
        .map(Versions.toList)
        .map(versions => DeleteContractResponseDTO(subject, versions))
        .value
    })
  
  private val getServerEndpoints: List[ServerEndpoint[Any, F]] =
    List(createContractSE, getContractVersionSE, getVersionsSE, getSubjectsSE, getLatestContractSE, deleteContractVersionSE, deleteContractSE)

  val serverEndpoints = getServerEndpoints
