package io.github.sergeiionin.contractsregistrator
package schemaregistry

import io.circe.{Encoder, Decoder}

enum KeyType derives Encoder, Decoder:
  case SCHEMA, DELETE_SUBJECT, NOOP, UNKNOWN

object KeyType:
  def fromString(s: String): KeyType = s match
    case "SCHEMA" => SCHEMA
    case "DELETE_SUBJECT" => DELETE_SUBJECT
    case "NOOP" => NOOP
    case _ => UNKNOWN