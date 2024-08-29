package io.github.sergeiionin.contractsregistrator
package serverendpoints

import domain.ContractPullRequest
import dto.*
import dto.errors.{BadRequestDTO, HttpErrorDTO}
import dto.github.webhooks.PrWebhookResponseDTO
import endpoints.WebhooksEndpoints
import producer.EventsProducer
import service.ContractService
import service.prs.PrService

import cats.effect.Async
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ServerEndpoint.Full

class WebhooksPrsServerEndpoints[F[_] : Async : Logger](
                                                         prsService: PrService[F]
                                                       ) extends WebhooksEndpoints:
  private val logger = summon[Logger[F]]
  
  private val pullRequestSE: ServerEndpoint[Any, F] =
    pullRequest.serverLogic(pr => {
      val isMerged = pr.pull_request.merged
      val body = pr.pull_request.body
      val isDeleted = pr.pull_request.title.toLowerCase.startsWith("delete")
      val contractPullRequest = ContractPullRequest.fromRaw(body, isDeleted)

      contractPullRequest match
        case Left(err) => 
          logger.error(s"Failed to parse contract pull request: $err") *>
            BadRequestDTO(msg = s"PR body is invalid: $body").asLeft[PrWebhookResponseDTO].pure[F]
        case Right(contractPr) =>
          logger.info(s"Received PR: $pr") *> {
            val response = PrWebhookResponseDTO(body, isMerged)
            if (pr.isClosed) {
              prsService.processPR(contractPr).as(response).value
            } else
              response.asRight[HttpErrorDTO].pure[F]
          }
    })
  
  private val getServerEndpoints: List[ServerEndpoint[Any, F]] =
    List(pullRequestSE)

  val serverEndpoints = getServerEndpoints
