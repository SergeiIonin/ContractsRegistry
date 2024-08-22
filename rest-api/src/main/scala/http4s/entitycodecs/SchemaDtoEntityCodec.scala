package io.github.sergeiionin.contractsregistrator
package http4s.entitycodecs

import dto.schema.SchemaDTO

import cats.effect.Concurrent
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

object SchemaDtoEntityCodec:
  given schemaDtoEncoder[F[_]]: EntityEncoder[F, SchemaDTO] = jsonEncoderOf[F, SchemaDTO]
  given schemaDtoDecoder[F[_] : Concurrent]: EntityDecoder[F, SchemaDTO] = jsonOf[F, SchemaDTO]
  
