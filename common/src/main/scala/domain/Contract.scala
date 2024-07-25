package io.github.sergeiionin.contractsregistrator
package domain

import io.circe.{Encoder, Decoder, Json}


final case class Contract(
                         subject: String,
                         version: Int,
                         id: Int,
                         schema: String
)

object Contract:
  given encoder: Encoder[Contract] = Encoder.derived[Contract]
  given decoder: Decoder[Contract] = Decoder.derived[Contract]
