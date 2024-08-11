package io.github.sergeiionin.contractsregistrator
package dto.schemaregistry

import io.circe.{Encoder, Decoder}
import sttp.tapir.Schema

object DTO:
  final case class CreateSchemaResponseDTO(id: Int) derives Encoder, Decoder, Schema
