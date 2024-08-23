package io.github.sergeiionin.contractsregistrator
package domain

import io.circe.{Decoder, Encoder, Json}
import io.circe.generic.semiauto.deriveDecoder

final case class Contract(
                         subject: String,
                         version: Int,
                         id: Int,
                         schema: String,
                         schemaType: SchemaType,
                         isMerged: Boolean = false,
                         deleted: Option[Boolean] = None
) derives Encoder

object Contract:
  given contractDecoder: Decoder[Contract] = deriveDecoder[Contract].prepare { cursor =>
    cursor.withFocus { json =>
      json.hcursor.downField("isMerged").as[Boolean] match {
        case Left(_) => json.mapObject(_.add("isMerged", Json.fromBoolean(false)))
        case Right(_) => json
      }
    }
  }


final case class SubjectAndVersion(subject: String, version: Int) derives Encoder, Decoder
