package io.github.sergeiionin.contractsregistrator
package http4s.entitycodecs

import domain.Versions

import cats.effect.Concurrent
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

object VersionsEntityCodec:
  given versionsEncoder[F[_]]: EntityEncoder[F, Versions] =
    jsonEncoderOf[F, List[Int]].contramap(Versions.toList)
  given versionsDecoder[F[_]: Concurrent]: EntityDecoder[F, Versions] =
    jsonOf[F, List[Int]].map(Versions.apply)
