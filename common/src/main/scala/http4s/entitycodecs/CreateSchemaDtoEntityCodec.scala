package io.github.sergeiionin.contractsregistrator
package http4s.entitycodecs

import dto.schema.CreateSchemaDTO

import cats.effect.Concurrent
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

object CreateSchemaDtoEntityCodec:
  given schemaDtoEncoder[F[_]]: EntityEncoder[F, CreateSchemaDTO] =
    jsonEncoderOf[F, CreateSchemaDTO]
  given schemaDtoDecoder[F[_]: Concurrent]: EntityDecoder[F, CreateSchemaDTO] =
    jsonOf[F, CreateSchemaDTO]
