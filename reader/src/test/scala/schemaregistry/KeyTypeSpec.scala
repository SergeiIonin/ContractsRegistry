package io.github.sergeiionin.contractsregistrator
package schemaregistry

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import io.circe.syntax.*
import io.circe.parser.*
import io.circe.generic.auto.*
import io.circe.generic.semiauto.*
import io.circe.{Encoder, Decoder}
import schemaregistry.KeyType.*

class KeyTypeSpec extends AnyWordSpec with Matchers:
  "KeyType" should {
    "be encoded and decoded correctly" in {
      val schema = SCHEMA
      val deleteSubject = DELETE_SUBJECT
      val noop = NOOP
      val unknown = UNKNOWN

      val schemaJson = schema.asJson
      val deleteSubjectJson = deleteSubject.asJson
      val noopJson = noop.asJson
      val unknownJson = unknown.asJson

      val schemaDecoded = schemaJson.as[KeyType]
      val deleteSubjectDecoded = deleteSubjectJson.as[KeyType]
      val noopDecoded = noopJson.as[KeyType]
      val unknownDecoded = unknownJson.as[KeyType]

      schemaDecoded shouldEqual Right(schema)
      deleteSubjectDecoded shouldEqual Right(deleteSubject)
      noopDecoded shouldEqual Right(noop)
      unknownDecoded shouldEqual Right(unknown)
    }
  }