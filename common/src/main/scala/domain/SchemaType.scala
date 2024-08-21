package io.github.sergeiionin.contractsregistrator
package domain

import io.circe.{Decoder, Encoder, Json, HCursor}
import sttp.tapir.Schema

enum SchemaType derives Schema:
  case AVRO, JSON, PROTOBUF, OTHER

object SchemaType:
  def fromString(s: String): SchemaType = 
    s match
      case "AVRO" => AVRO
      case "JSON" => JSON
      case "PROTOBUF" => PROTOBUF
      case _ => OTHER

  given Encoder[SchemaType] with
    def apply(a: SchemaType): Json = Json.fromString(a.toString)

  given Decoder[SchemaType] with
    def apply(c: HCursor): Decoder.Result[SchemaType] =
      c.as[String].map(fromString)