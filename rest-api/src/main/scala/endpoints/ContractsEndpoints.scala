package io.github.sergeiionin.contractsregistrator
package endpoints

import dto.*
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.Schema
import sttp.model.StatusCode

trait ContractsEndpoints:
  import ContractsEndpoints.*
  given createContractSchema: Schema[CreateContractDTO] = Schema.derived[CreateContractDTO]
  given createContractResponseSchema: Schema[CreateContractResponseDTO] = Schema.derived[CreateContractResponseDTO]
  
  private val base =
    endpoint
    .in(contracts)
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
      .in(`:subject`)
      .in(versions)
      .in(`:version`)
      .out(jsonBody[DeleteContractVersionResponseDTO])
      .name(ContractEndpoint.DeleteContractVersion.toString)
      .description("Delete a contracts version")
  
  val deleteContract =
    base.delete
      .in(`:subject`)
      .out(jsonBody[DeleteContractResponseDTO])
      .name(ContractEndpoint.DeleteContractSubject.toString)
      .description("Delete the contract")
  
  val getContractVersion =
    base.get
      .in(`:subject`)
      .in(versions)
      .in(`:version`)
      .out(jsonBody[ContractDTO])
      .name(ContractEndpoint.GetContractVersion.toString)
      .description("Get a contract by subject and version")

  val getVersions =
    base.get
      .in(`:subject`)
      .in(versions)
      .out(jsonBody[List[Int]])
      .name(ContractEndpoint.GetVersions.toString)
      .description("Get all versions of a contract")
  
  val getSubjects =
    base.get
      .in(subjects)
      .out(jsonBody[List[String]])
      .name(ContractEndpoint.GetSubjects.toString)
      .description("Get all subjects")
  
  val getContracts =
    base.get
      .in(subjects)
      .out(jsonBody[List[ContractDTO]])
      .name(ContractEndpoint.GetContracts.toString)
      .description("Get all contracts")

  def getEndpoints: List[AnyEndpoint] =
    List(createContract, deleteContractVersion, deleteContract, getContractVersion, getVersions, getContracts)

object ContractsEndpoints:
  val contracts = "contracts"
  val versions = "versions"
  val subjects = "subjects"
  val `:subject` = path[String]("subject")
  val `:version` = path[Int]("version")