package io.github.sergeiionin.contractsregistrator
package dto.schema

import io.circe.{Decoder, Encoder}
import domain.SchemaType
import sttp.tapir.Schema

final case class CreateSchemaResponseDTO(id: Int) derives Encoder, Decoder, Schema
final case class CreateSchemaDTO(schemaType: String = "PROTOBUF", schema: String) derives Encoder, Decoder, Schema

final case class SchemaDTO(subject: String, version: Int, id: Int, schemaType: SchemaType, schema: String) derives Encoder, Decoder, Schema
