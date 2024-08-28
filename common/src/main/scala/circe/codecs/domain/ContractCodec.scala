package io.github.sergeiionin.contractsregistrator
package circe.codecs.domain

import domain.Contract

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder, Json}

object ContractCodec:
  given contractEncoder: Encoder[Contract] = deriveEncoder[Contract]
  given contractDecoder: Decoder[Contract] = deriveDecoder[Contract].prepare { cursor =>
    cursor.withFocus { json =>
      json.hcursor.downField("isMerged").as[Boolean] match {
        case Left(_) => json.mapObject(_.add("isMerged", Json.fromBoolean(false)))
        case Right(_) => json
      }
    }
  }
