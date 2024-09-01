package io.github.sergeiionin.contractsregistrator
package endpoints

import dto.*
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.Schema
import sttp.model.StatusCode

trait DeleteContractEndpoints extends RootContractsEndpoint:

  val deleteContractVersion =
    root
      .delete
      .in(`:subject`)
      .in(versions)
      .in(`:version`)
      .out(jsonBody[DeleteContractVersionResponseDTO])
      .name(ContractEndpoint.DeleteContractVersion.toString)
      .description("Delete a contracts version")

  val deleteContract =
    root
      .delete
      .in(`:subject`)
      .out(jsonBody[DeleteContractResponseDTO])
      .name(ContractEndpoint.DeleteContractSubject.toString)
      .description("Delete the contract")

  def getEndpoints: List[AnyEndpoint] =
    List(deleteContractVersion, deleteContract)
