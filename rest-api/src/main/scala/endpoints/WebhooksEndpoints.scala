package io.github.sergeiionin.contractsregistrator
package endpoints

import dto.github.webhooks.{PrWebhookRequestDTO, PrWebhookResponseDTO}
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*
import dto.errors.{HttpErrorDTO, BadRequestDTO}

trait WebhooksEndpoints:
  given PullRequestDtoSchema: Schema[PrWebhookRequestDTO] = Schema.derived[PrWebhookRequestDTO]
  
  private val base =
    endpoint
    .in("webhooks") // for tests w/ ngrok free plan, hide this and next lines  as subdomains won't be supported 
    .in("prs")
    .errorOut(
        oneOf[HttpErrorDTO](
          oneOfVariant(StatusCode.BadRequest, jsonBody[BadRequestDTO])
        )
    )
  
  val pullRequest =
    base.post
      .in(jsonBody[PrWebhookRequestDTO])
      .out(jsonBody[PrWebhookResponseDTO])
      .name("PullRequest")
      .description("Intercept pull request event from GitHub webhooks")
  
  def getEndpoints: List[AnyEndpoint] = 
    List(pullRequest)