package io.github.sergeiionin.contractsregistrator
package serverendpoints

import domain.ContractPullRequest
import dto.*
import dto.github.webhooks.{PrErrorDTO, PrWebhookResponseDTO, BadRequestErrorDTO as PrBadRequestErrorDTO}
import endpoints.WebhooksEndpoints
import producer.EventsProducer
import domain.events.prs.{PrClosed, PrClosedKey}
import cats.effect.Async
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ServerEndpoint.Full

class WebhooksPrsServerEndpoints[F[_] : Async : Logger](
                                                         producer: EventsProducer[F, PrClosedKey, PrClosed]
                                                       ) extends WebhooksEndpoints:
  private val logger = summon[Logger[F]]
  
  private val pullRequestSE: ServerEndpoint[Any, F] =
    pullRequest.serverLogic(pr => {
      val isMerged = pr.pull_request.merged
      val body = pr.pull_request.body
      val contractPullRequest = ContractPullRequest.fromRaw(body, isMerged)

      contractPullRequest match
        case Left(err) => 
          logger.error(s"Failed to parse contract pull request: $err") *>
            PrBadRequestErrorDTO(s"PR body is invalid: $body").asLeft[PrWebhookResponseDTO].pure[F]
        case Right(contractPr) =>
          logger.info(s"Received PR: $pr") *> {
            val response = PrWebhookResponseDTO(body, isMerged)
            if (pr.isClosed) {
              val key = PrClosedKey(contractPr.subject, contractPr.version)
              val msg = PrClosed(contractPr.subject, contractPr.version, isMerged)
              producer.produce(key, msg)
                .as(response.asRight[PrErrorDTO])
            } else
              response.asRight[PrErrorDTO].pure[F]
          }
    })
  
  private val getServerEndpoints: List[ServerEndpoint[Any, F]] =
    List(pullRequestSE)

  val serverEndpoints = getServerEndpoints
