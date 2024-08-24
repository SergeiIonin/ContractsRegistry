package io.github.sergeiionin.contractsregistrator
package endpoints

import dto.errors.{HttpErrorDTO, BadRequestDTO}
import sttp.tapir.*
import sttp.model.StatusCode
import sttp.tapir.json.circe.*

trait BaseContractsEndpoint:
  
  val base =
    endpoint
      .in(contracts)
      .errorOut(
        oneOf[HttpErrorDTO](
          oneOfVariant(StatusCode.BadRequest, jsonBody[BadRequestDTO])
        )
      )

  val contracts = "contracts"
  
  val versions = "versions"
  
  val subjects = "subjects"
  
  val latest = "latest"
  
  val `:subject` = path[String]("subject")
  
  val `:version` = path[Int]("version")
