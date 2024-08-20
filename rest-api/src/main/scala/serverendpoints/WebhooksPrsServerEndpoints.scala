package io.github.sergeiionin.contractsregistrator
package serverendpoints

import domain.ContractPullRequest
import dto.*
import dto.github.webhooks.{PrErrorDTO, PrWebhookResponseDTO, BadRequestErrorDTO as PrBadRequestErrorDTO}
import endpoints.WebhooksEndpoints
import http.client.ContractsRegistryHttpClient
import producer.GitHubEventsProducer
import domain.events.prs.{PrClosed, PrClosedKey}
import repository.ContractsRepository

import cats.effect.Concurrent
import cats.effect.kernel.Async
import cats.syntax.all.*
import cats.syntax.monad.*
import cats.{Monad, MonadThrow}
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder, Uri}
import org.typelevel.log4cats.Logger
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ServerEndpoint.Full

class WebhooksPrsServerEndpoints[F[_] : Async : MonadThrow : Logger](
                                                                      producer: GitHubEventsProducer[F, PrClosedKey, PrClosed]
                                                                    ) extends WebhooksEndpoints:
  import ContractsServerEndpoints.given
  
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
          val response = PrWebhookResponseDTO(body, isMerged)
          logger.info(s"Received PR: $pr") *>
            producer.produce(PrClosedKey(contractPr.subject, contractPr.version),
                              PrClosed(contractPr.subject, contractPr.version, isMerged)) *> // todo handle errors here?
              response.asRight[PrErrorDTO].pure[F]
    })
  
  private val getServerEndpoints: List[ServerEndpoint[Any, F]] =
    List(pullRequestSE)

  val serverEndpoints = getServerEndpoints
