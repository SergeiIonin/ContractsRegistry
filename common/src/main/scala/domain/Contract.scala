package io.github.sergeiionin.contractsregistrator
package domain

import io.circe.{Encoder, Decoder, Json}


final case class Contract(
                         name: String,
                         description: Option[String],
                         fields: Map[String, Any]
                         )

object Contract:
  implicit val encodeFields: Encoder[Map[String, Any]] = Encoder.encodeMap[String, Json].contramap(_.view.mapValues(_.asInstanceOf[Json]))
  implicit val decodeFields: Decoder[Map[String, Any]] = Decoder.decodeMap[String, Json].map(_.view.mapValues(_.asInstanceOf[Any]))

