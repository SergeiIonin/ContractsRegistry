package io.github.sergeiionin.contractsregistrator
package http4s.entitycodecs

import dto.schema.CreateSchemaResponseDTO

import cats.effect.Concurrent
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

object CreateSchemaResponseDtoEntityCodec:
  given createSchemaResponseDtoEncoder[F[_]]: EntityEncoder[F, CreateSchemaResponseDTO] = jsonEncoderOf[F, CreateSchemaResponseDTO]
  given createSchemaResponseDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, CreateSchemaResponseDTO] = jsonOf[F, CreateSchemaResponseDTO]
