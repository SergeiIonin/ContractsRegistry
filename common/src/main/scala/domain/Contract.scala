package io.github.sergeiionin.contractsregistrator
package domain

import io.circe.{Encoder, Decoder, Json}


final case class Contract(
                         name: String,
                         description: Option[String],
                         fields: Map[String, Any]
                         )

object Contract:
  given encodeFields: Encoder[Map[String, Any]] = Encoder.encodeMap[String, Json].contramap(_.view.mapValues(_.asInstanceOf[Json]).toMap)
  given decodeFields: Decoder[Map[String, Any]] = Decoder.decodeMap[String, Json].map(_.view.mapValues(_.asInstanceOf[Any]).toMap)
  given encoder: Encoder[Contract] = Encoder.derived[Contract]
  given decoder: Decoder[Contract] = Decoder.derived[Contract]
