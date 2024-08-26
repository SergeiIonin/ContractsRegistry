/*package io.github.sergeiionin.contractsregistrator
package schemaregistry.schemasconsumer

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import cats.Id
import cats.data.NonEmptyList
import fs2.kafka.CommittableConsumerRecord
import io.circe.syntax.*
import io.circe.parser.*
import io.circe.generic.auto.*
import io.circe.generic.semiauto.*
import io.circe.{Encoder, Decoder}
import schemaregistry.KeyType.*

class SchemasKafkaConsumerSpec extends AnyWordSpec with Matchers:
  val schemasConsumer =
    new SchemasConsumer[Id]:
      def subscribe(topics: NonEmptyList[String]): Id[Unit] = ()
      def stream(): fs2.Stream[Id, CommittableConsumerRecord[Id, Bytes, Bytes]] = fs2.Stream.empty
  
  "SchemasKafkaConsumer" should {
    "be able to decode schema key" in {
      val rawKeySchema: String =
        """
          |{
          |   "keytype":"SCHEMA",
          |   "subject":"contracts",
          |   "version":2,
          |   "magic":1
          |}
          |""".stripMargin

      val rawKeyDeleteSubject: String =
        """
          |{
          |    "keytype":"DELETE_SUBJECT",
          |    "subject":"foo",
          |    "magic":0
          |}
          |""".stripMargin

      val rawKeyNOOP: String =
        """
          |{
          |    "keytype":"NOOP",
          |    "magic":0
          |}
          |""".stripMargin

      val rawKeyOther: String =
        """
          |{
          |    "keytype":"Entered_The_Wrong_Door",
          |    "magic":0
          |}
          |""".stripMargin

      val schemaKey = schemasConsumer.getRecordKeyType(rawKeySchema)
      val deleteSubjectKey = schemasConsumer.getRecordKeyType(rawKeyDeleteSubject)
      val noopKey = schemasConsumer.getRecordKeyType(rawKeyNOOP)
      val otherKey = schemasConsumer.getRecordKeyType(rawKeyOther)

      schemaKey shouldEqual SCHEMA
      deleteSubjectKey shouldEqual DELETE_SUBJECT
      noopKey shouldEqual NOOP
      otherKey shouldEqual OTHER
    }
  }*/