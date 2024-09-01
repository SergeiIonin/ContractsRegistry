package io.github.sergeiionin.contractsregistrator
package endpoints

import dto.*
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.Schema
import sttp.model.StatusCode

trait CreateContractEndpoints extends RootContractsEndpoint:

  val createContract =
    root
      .post
      .in(jsonBody[CreateContractDTO])
      .out(jsonBody[CreateContractResponseDTO])
      .name(ContractEndpoint.CreateContract.toString)
      .description("Create new contract")

  def getEndpoints: List[AnyEndpoint] =
    List(createContract)
