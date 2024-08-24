package io.github.sergeiionin.contractsregistrator
package serverendpoints

import dto.*
import dto.schema.*
import endpoints.CreateContractEndpoints
import client.CreateSchemaClient
import cats.effect.Async
import cats.syntax.functor.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ServerEndpoint.Full

final class CreateContractServerEndpoints[F[_] : Async](client: CreateSchemaClient[F]) extends CreateContractEndpoints:

  private val createContractSE: ServerEndpoint[Any, F] =
    createContract.serverLogic(createContract => {
      val createSchemaDTO = CreateSchemaDTO(schema = createContract.schema)
      client
        .createSchema(createContract.subject, createSchemaDTO)
        .map(response => CreateContractResponseDTO(createContract.subject, response.id))
        .value
    })
  
  private val getServerEndpoints: List[ServerEndpoint[Any, F]] =
    List(createContractSE)

  val serverEndpoints = getServerEndpoints
