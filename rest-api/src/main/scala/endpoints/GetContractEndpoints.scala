package io.github.sergeiionin.contractsregistrator
package endpoints

import dto.*
import sttp.tapir.*
import sttp.tapir.json.circe.*
import sttp.tapir.Schema
import sttp.model.StatusCode

trait GetContractEndpoints extends BaseContractsEndpoint:
  
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
  
  val getLatestContract =
    base.get
      .in(`:subject`)
      .in(latest)
      .out(jsonBody[ContractDTO])
      .name(ContractEndpoint.GetLatestContract.toString)
      .description("Get the latest contract version")
  
  def getEndpoints: List[AnyEndpoint] =
    List(getContractVersion, getVersions, getSubjects, getLatestContract)
