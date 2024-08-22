package io.github.sergeiionin.contractsregistrator
package http4s.entitycodecs

import dto.schema.Version

import cats.effect.Concurrent
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.{EntityDecoder, EntityEncoder}

object VersionEntityCodec:
  given versionEncoder[F[_]]: EntityEncoder[F, Version] = jsonEncoderOf[F, Int].contramap(Version.toInt)
  given versionDecoder[F[_] : Concurrent]: EntityDecoder[F, Version] = jsonOf[F, Int].map(Version.apply)
