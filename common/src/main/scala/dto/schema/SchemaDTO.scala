package io.github.sergeiionin.contractsregistrator
package dto.schema

import io.circe.{Encoder, Decoder}
import sttp.tapir.Schema

final case class CreateSchemaResponseDTO(id: Int) derives Encoder, Decoder, Schema
final case class SchemaDTO(schemaType: String = "PROTOBUF", schema: String)derives Encoder, Decoder, Schema
