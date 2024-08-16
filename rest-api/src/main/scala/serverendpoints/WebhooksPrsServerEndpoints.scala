package io.github.sergeiionin.contractsregistrator
package serverendpoints

import dto.*
import dto.schemaregistry.DTO.*
import endpoints.WebhooksEndpoints
import http.client.ContractsRegistryHttpClient
import repository.ContractsRepository

import cats.{Monad, MonadThrow}
import cats.effect.Concurrent
import cats.effect.kernel.Async
import cats.syntax.all.*
import cats.syntax.monad.*
import dto.github.webhooks.{PrErrorDTO, PrWebhookResponseDTO, BadRequestErrorDTO as PrBadRequestErrorDTO}
import domain.ContractPullRequest

import io.github.sergeiionin.contractsregistrator.producer.GitHubEventsProducer
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder, Uri}
import org.typelevel.log4cats.Logger
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ServerEndpoint.Full

class WebhooksServerEndpoints[F[_] : Async : MonadThrow : Logger](producer: GitHubEventsProducer[F, ]) extends WebhooksEndpoints:
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
        case Right(_) =>
          val response = PrWebhookResponseDTO(body, isMerged)
          logger.info(s"Received PR: $pr") *>
            response.asRight[PrErrorDTO].pure[F]
    })
  
  private val getServerEndpoints: List[ServerEndpoint[Any, F]] =
    List(pullRequestSE)

  val serverEndpoints = getServerEndpoints
