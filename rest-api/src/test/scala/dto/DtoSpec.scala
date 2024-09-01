package io.github.sergeiionin.contractsregistrator
package dto

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.circe.{Decoder, Encoder}
import io.circe.syntax.*
import io.circe.parser.decode
import domain.SchemaType

class DtoSpec extends AnyWordSpec with Matchers:
  "DTO" should {
    "decode ContractDTO" in {
      val json =
        """
          |{
          |  "subject" : "contracts",
          |  "version" : 2,
          |  "id"      : 3, 
          |  "schemaType" : "PROTOBUF",
          |  "schema": "syntax = \"proto3\";\npackage schema_registry;\n\nmessage SchemaMessage {\n  string subject = 1;\n  int32 version = 2;\n  int32 id = 3;\n  string schema = 4;\n  bool deleted = 5;\n}\n"
          |}
          |""".stripMargin
      val contractDto = decode[ContractDTO](json)
      contractDto shouldBe Right(
        ContractDTO(
          "contracts",
          2,
          SchemaType.PROTOBUF,
          "syntax = \"proto3\";\npackage schema_registry;\n\nmessage SchemaMessage {\n  string subject = 1;\n  int32 version = 2;\n  int32 id = 3;\n  string schema = 4;\n  bool deleted = 5;\n}\n"
        ))
    }
  }
