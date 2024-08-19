package io.github.sergeiionin.contractsregistrator
package endpoints

import dto.*
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
      .name("CreateContract")
      .description("Create new contract")
  
  val deleteContractVersion =
    base.delete
      .in(path[String]("name"))
      .in("versions")
      .in(path[Int]("version"))
      .out(jsonBody[DeleteContractVersionResponseDTO])
      .name("DeleteContractVersion")
      .description("Delete a contracts version")
  
  val deleteContract =
    base.delete
      .in(path[String]("contract"))
      .out(jsonBody[DeleteContractResponseDTO])
      .name("DeleteContract")
      .description("Delete the contract")
  
  val getContract =
    base.get
      .in(path[String]("name"))
      .in(path[Int]("id").default(1))
      .out(jsonBody[ContractDTO])
      .name("GetContract")
      .description("Get a contract by subject and version")
  
  val getContracts =
    base.get
      .in("contracts")
      .out(jsonBody[List[ContractDTO]])
      .name("GetContracts")
      .description("Get all contracts")
  
  def getEndpoints: List[AnyEndpoint] = 
    List(createContract, deleteContractVersion, deleteContract, getContract, getContracts)