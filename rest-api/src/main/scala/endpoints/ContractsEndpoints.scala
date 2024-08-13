package io.github.sergeiionin.contractsregistrator
package endpoints

import io.github.sergeiionin.contractsregistrator.dto.*
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.Schema
import sttp.model.StatusCode

trait ContractsEndpoints:
  given createContractSchema: Schema[CreateContractDTO] = Schema.derived[CreateContractDTO]
  given createContractResponseSchema: Schema[CreateContractResponseDTO] = Schema.derived[CreateContractResponseDTO]
  
  private val base =
    endpoint
    .in("contracts")
    .errorOut(
        oneOf[ContractErrorDTO](
          oneOfVariant(StatusCode.BadRequest, jsonBody[BadRequestDTO])
        )
    )
  
  val createContract =
    base.post
      .in(jsonBody[CreateContractDTO])
      .out(jsonBody[CreateContractResponseDTO])
  
  val deleteContractVersion =
    base.delete
      .in(path[String]("name"))
      .in("versions")
      .in(path[Int]("version"))
      .out(jsonBody[DeleteContractVersionResponseDTO])  
  
  val deleteContract =
    base.delete
      .in(path[String]("contract"))
      .out(jsonBody[DeleteContractResponseDTO])
  
  val getContract =
    base.get
      .in(path[String]("name"))
      .in(path[Int]("id").default(1))
      .out(jsonBody[ContractDTO])
  
  val getContracts =
    base.get
      .in("contracts")
      .out(jsonBody[List[ContractDTO]])
  
  def getEndpoints: List[AnyEndpoint] = 
    List(createContract, deleteContractVersion, deleteContract, getContract, getContracts)