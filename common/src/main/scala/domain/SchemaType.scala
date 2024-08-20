package io.github.sergeiionin.contractsregistrator
package domain

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

enum SchemaType derives Encoder, Decoder, Schema:
  case AVRO, JSON, PROTOBUF, OTHER

object SchemaType:
  def fromString(s: String): SchemaType = 
    s match
      case "AVRO" => AVRO
      case "JSON" => JSON
      case "PROTOBUF" => PROTOBUF
      case _ => OTHER