package io.github.sergeiionin.contractsregistrator
package domain

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import io.circe.syntax.*
import io.circe.parser.decode
import circe.codecs.domain.ContractCodec.given

class ContractSpec extends AnyWordSpec with Matchers:
  "Contract" should {
    "be encoded" in {
      val contract = Contract("testSubject", 1, 123, "schema", SchemaType.PROTOBUF, None)
      contract.asJson.noSpaces shouldEqual
        """{"subject":"testSubject","version":1,"id":123,"schema":"schema","schemaType":"PROTOBUF","deleted":null}"""
    }
    "be decoded" in {
      val raw =
        """
          |{
          |  "subject" : "testSubject",
          |  "version" : 1,
          |  "id"      : 123, 
          |  "schemaType" : "PROTOBUF",
          |  "schema": "syntax = \"proto3\";\npackage schema_registry;\n\nmessage SchemaMessage {\n  string subject = 1;\n  int32 version = 2;\n  int32 id = 3;\n  string schema = 4;\n  bool deleted = 5;\n}\n",
          |  "deleted": false
          |}
          |""".stripMargin
      val decoded = decode[Contract](raw)
      decoded.toOption shouldEqual Some(
        Contract(
          "testSubject",
          1,
          123,
          "syntax = \"proto3\";\npackage schema_registry;\n\nmessage SchemaMessage {\n  string subject = 1;\n  int32 version = 2;\n  int32 id = 3;\n  string schema = 4;\n  bool deleted = 5;\n}\n",
          SchemaType.PROTOBUF,
          Some(false)
        ))
    }
  }
