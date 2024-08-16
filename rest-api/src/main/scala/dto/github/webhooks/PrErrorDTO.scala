package io.github.sergeiionin.contractsregistrator
package dto.github.webhooks

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

sealed trait PrErrorDTO derives Encoder, Decoder, Schema

final case class BadRequestErrorDTO(msg: String) extends PrErrorDTO derives Encoder, Decoder, Schema
