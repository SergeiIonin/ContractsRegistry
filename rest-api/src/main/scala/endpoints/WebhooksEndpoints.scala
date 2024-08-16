package io.github.sergeiionin.contractsregistrator
package endpoints

import dto.github.webhooks.{PrWebhookRequestDTO, PrWebhookResponseDTO, PrErrorDTO, BadRequestErrorDTO}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

trait WebhooksEndpoints:
  given PullRequestDtoSchema: Schema[PrWebhookRequestDTO] = Schema.derived[PrWebhookRequestDTO]
  
  private val base =
    endpoint
    .in("webhooks") // for tests w/ ngrok free plan, hide this and next lines  as subdomains won't be supported 
    .in("prs")
    .errorOut(
        oneOf[PrErrorDTO](
          oneOfVariant(StatusCode.BadRequest, jsonBody[BadRequestErrorDTO])
        )
    )
  
  val pullRequest =
    base.post
      .in(jsonBody[PrWebhookRequestDTO])
      .out(jsonBody[PrWebhookResponseDTO])
  
  def getEndpoints: List[AnyEndpoint] = 
    List(pullRequest)