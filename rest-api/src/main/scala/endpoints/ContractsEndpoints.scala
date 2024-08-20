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
      .name(ContractEndpoint.CreateContract.toString)
      .description("Create new contract")
  
  val deleteContractVersion =
    base.delete
      .in(path[String]("subject"))
      .in("versions")
      .in(path[Int]("version"))
      .out(jsonBody[DeleteContractVersionResponseDTO])
      .name(ContractEndpoint.DeleteContractVersion.toString)
      .description("Delete a contracts version")
  
  val deleteContract =
    base.delete
      .in(path[String]("subject"))
      .out(jsonBody[DeleteContractResponseDTO])
      .name(ContractEndpoint.DeleteContractSubject.toString)
      .description("Delete the contract")
  
  val getContract =
    base.get
      .in(path[String]("name"))
      .in(path[Int]("id").default(1))
      .out(jsonBody[ContractDTO])
      .name(ContractEndpoint.GetContract.toString)
      .description("Get a contract by subject and version")
  
  val getContracts =
    base.get
      .in("contracts")
      .out(jsonBody[List[ContractDTO]])
      .name(ContractEndpoint.GetContracts.toString)
      .description("Get all contracts")
  
  def getEndpoints: List[AnyEndpoint] = 
    List(createContract, deleteContractVersion, deleteContract, getContract, getContracts)
