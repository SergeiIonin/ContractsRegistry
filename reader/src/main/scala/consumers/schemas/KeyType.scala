package io.github.sergeiionin.contractsregistrator
package consumers.schemas

import io.circe.{Encoder, Decoder}

enum KeyType derives Encoder, Decoder:
  case SCHEMA, DELETE_SUBJECT, NOOP, OTHER

object KeyType:
  def fromString(s: String): KeyType = s match
    case "SCHEMA" => SCHEMA
    case "DELETE_SUBJECT" => DELETE_SUBJECT
    case "NOOP" => NOOP
    case _ => OTHER
