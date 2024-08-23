package io.github.sergeiionin.contractsregistrator
package dto
package errors

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

sealed trait HttpErrorDTO derives Encoder, Decoder, Schema

final case class BadRequestDTO(code: Int, msg: String) extends HttpErrorDTO derives Encoder, Decoder, Schema
final case class InternalServerErrorDTO(code: Int = 500) extends HttpErrorDTO derives Encoder, Decoder, Schema

