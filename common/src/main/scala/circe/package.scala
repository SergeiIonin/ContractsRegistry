package io.github.sergeiionin.contractsregistrator

import io.circe.{Decoder, parser, Error, DecodingFailure}

package object circe:
  def parseRaw[R: Decoder](raw: Option[String]): Either[Error, R] =
    raw match
      case None => Left(DecodingFailure("Record is empty", List.empty))
      case Some(r) =>
        parser.parse(r).flatMap(json => {
          json.as[R]
        })