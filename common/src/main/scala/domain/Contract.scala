package io.github.sergeiionin.contractsregistrator
package domain

import io.circe.{Encoder, Decoder, Json}


final case class Contract(
                         subject: String,
                         version: Int,
                         id: Int,
                         schema: String,
                         deleted: Option[Boolean] = None
)

object Contract:
  given encoder: Encoder[Contract] = Encoder.derived[Contract]
  given decoder: Decoder[Contract] = Decoder.derived[Contract]

final case class SubjectAndVersion(subject: String, version: Int)

object SubjectAndVersion:
  given encoder: Encoder[SubjectAndVersion] = Encoder.derived[SubjectAndVersion]
  given decoder: Decoder[SubjectAndVersion] = Decoder.derived[SubjectAndVersion]