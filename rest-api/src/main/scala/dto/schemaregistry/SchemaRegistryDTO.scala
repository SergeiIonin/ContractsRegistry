package io.github.sergeiionin.contractsregistrator
package dto.schemaregistry

import cats.effect.Concurrent
import io.circe.{Encoder, Decoder}
import sttp.tapir.Schema
import org.http4s.{EntityEncoder, EntityDecoder}
import org.http4s.circe.{jsonEncoderOf, jsonOf}

final case class CreateSchemaResponseDTO(id: Int) derives Encoder, Decoder, Schema
final case class SchemaDTO(schemaType: String = "PROTOBUF", schema: String)derives Encoder, Decoder, Schema

object SchemaRegistryDTO:
  given schemaDtoEncoder[F[_]]: EntityEncoder[F, SchemaDTO] = jsonEncoderOf[F, SchemaDTO]
  given schemaDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, SchemaDTO] = jsonOf[F, SchemaDTO]
  given createSchemaResponseDtoEncoder[F[_]]: EntityEncoder[F, CreateSchemaResponseDTO] = jsonEncoderOf[F, CreateSchemaResponseDTO]
  given createSchemaResponseDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, CreateSchemaResponseDTO] = jsonOf[F, CreateSchemaResponseDTO]