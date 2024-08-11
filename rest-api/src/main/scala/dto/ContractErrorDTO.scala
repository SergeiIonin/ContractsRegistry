package io.github.sergeiionin.contractsregistrator
package dto

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

sealed trait ContractErrorDTO derives Encoder, Decoder, Schema

final case class BadRequestDTO(name: String, msg: String) extends ContractErrorDTO derives Encoder, Decoder, Schema

