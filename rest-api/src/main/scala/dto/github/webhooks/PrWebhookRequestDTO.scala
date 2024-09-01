package io.github.sergeiionin.contractsregistrator
package dto.github.webhooks

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

final case class PrWebhookRequestDTO(
    action: String,
    number: Int,
    pull_request: PullRequest
) derives Encoder,
      Decoder,
      Schema:
  def isClosed: Boolean = action == "closed"
  def isMerged: Boolean = isClosed && pull_request.merged
  def isRejected: Boolean = isClosed && !pull_request.merged

final case class PullRequest(
    url: String,
    id: Int,
    merged: Boolean,
    state: String,
    title: String,
    body: String
) derives Encoder,
      Decoder,
      Schema

final case class PrWebhookResponseDTO(body: String, isMerged: Boolean)
    derives Encoder,
      Decoder,
      Schema
