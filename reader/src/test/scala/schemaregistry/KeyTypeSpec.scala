package io.github.sergeiionin.contractsregistrator
package schemaregistry

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import io.circe.syntax.*
import io.circe.parser.*
import io.circe.generic.auto.*
import io.circe.generic.semiauto.*
import io.circe.{Encoder, Decoder}
import consumers.schemas.KeyType
import consumers.schemas.KeyType.*

class KeyTypeSpec extends AnyWordSpec with Matchers:
  "KeyType" should {
    "be encoded and decoded correctly" in {
      val schema = SCHEMA
      val deleteSubject = DELETE_SUBJECT
      val noop = NOOP
      val other = OTHER

      val schemaJson = schema.asJson
      val deleteSubjectJson = deleteSubject.asJson
      val noopJson = noop.asJson
      val otherJson = other.asJson

      val schemaDecoded = schemaJson.as[KeyType]
      val deleteSubjectDecoded = deleteSubjectJson.as[KeyType]
      val noopDecoded = noopJson.as[KeyType]
      val otherDecoded = otherJson.as[KeyType]

      schemaDecoded shouldEqual Right(schema)
      deleteSubjectDecoded shouldEqual Right(deleteSubject)
      noopDecoded shouldEqual Right(noop)
      otherDecoded shouldEqual Right(other)
    }
  }