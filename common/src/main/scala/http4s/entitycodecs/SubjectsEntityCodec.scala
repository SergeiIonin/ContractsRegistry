package io.github.sergeiionin.contractsregistrator
package http4s.entitycodecs

import dto.schema.Subjects

import cats.effect.Concurrent
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

object SubjectsEntityCodec:
  given subjectsEncoder[F[_]]: EntityEncoder[F, Subjects] = jsonEncoderOf[F, List[String]].contramap(Subjects.toList)
  given subjectsDecoder[F[_] : Concurrent]: EntityDecoder[F, Subjects] = jsonOf[F, List[String]].map(Subjects.apply)
