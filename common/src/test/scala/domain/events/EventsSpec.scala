package io.github.sergeiionin.contractsregistrator
package domain.events

import domain.events.contracts.*
import domain.{Contract, SchemaType}
import io.github.sergeiionin.contractsregistrator.circe.codecs.domain.events.contracts.ContractEventKeyCodec.given
import io.github.sergeiionin.contractsregistrator.circe.codecs.domain.events.contracts.ContractEventCodec.given
import io.circe.derivation.{Configuration, ConfiguredDecoder, ConfiguredEncoder}
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Decoder, Encoder}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class EventsSpec extends AnyWordSpec with Matchers:
  import domain.events.EventsSpec.MyEvent
  import domain.events.EventsSpec.MyEvent.given
  import EventsSpec.{*, given}
  "ContractEventKey" should {
    "encode and decode Bar" in {
      val bar: MyEvent = Bar(1, "hello")
      val baz: MyEvent = Baz("world")
      val subjectDeleted: MyEvent = SubjectDeleted("subject")
      val versionDeleted: MyEvent = VersionDeleted("subject", 1)

      // Encode as JSON
      val jsonBaz = baz.asJson.noSpaces
      val jsonBar = bar.asJson.noSpaces
      val jsonSubjectDeleted = subjectDeleted.asJson.noSpaces
      val jsonVersionDeleted = versionDeleted.asJson.noSpaces

      println(jsonBar)
      println(jsonBaz)
      println(jsonSubjectDeleted)
      println(jsonVersionDeleted)

      val decodedBar = decode[MyEvent](jsonBar)
      val decodedBaz = decode[MyEvent](jsonBaz)
      val decodedSubjectDeleted = decode[MyEvent](jsonSubjectDeleted)
      val decodedVersionDeleted = decode[MyEvent](jsonVersionDeleted)

      decodedBar shouldBe Right(bar)
      decodedBaz shouldBe Right(baz)
      decodedSubjectDeleted shouldBe Right(subjectDeleted)
      decodedVersionDeleted shouldBe Right(versionDeleted)
    }

    "specific encoders and decoders work" in {
      val bar: Bar = Bar(1, "hello")
      val baz: Baz = Baz("world")
      val subjectDeleted: MyEvent = SubjectDeleted("subject")
      val versionDeleted: MyEvent = VersionDeleted("subject", 1)

      given encoderBar: Encoder[Bar] = summon[Encoder[Bar]]
      given encoderBaz: Encoder[Baz] = summon[Encoder[Baz]]

      given decoderBar: Decoder[Bar] = summon[Decoder[Bar]]
      given decoderBaz: Decoder[Baz] = summon[Decoder[Baz]]

      // given decoderBar: Decoder[Bar] = summon[Decoder[Bar]]
      // Encode as JSON
      val jsonBaz = baz.asJson.noSpaces
      val jsonBar = bar.asJson.noSpaces
      val jsonSubjectDeleted = subjectDeleted.asJson.noSpaces
      val jsonVersionDeleted = versionDeleted.asJson.noSpaces

      println(jsonBar)
      println(jsonBaz)
      println(jsonSubjectDeleted)
      println(jsonVersionDeleted)

      val decodedBar = decode[Bar](jsonBar)
      val decodedBaz = decode[Baz](jsonBaz)
      val decodedSubjectDeleted = decode[MyEvent](jsonSubjectDeleted)
      val decodedVersionDeleted = decode[MyEvent](jsonVersionDeleted)

      decodedBar shouldBe Right(bar)
      decodedBaz shouldBe Right(baz)
      decodedSubjectDeleted shouldBe Right(subjectDeleted)
      decodedVersionDeleted shouldBe Right(versionDeleted)
    }

  }

object EventsSpec:

  sealed trait MyEvent
  // Define case classes extending the sealed trait
  final case class Bar(id: Int, msg: String) extends MyEvent
  final case class Baz(value: String) extends MyEvent
  // Circe configuration with a discriminator
  object MyEvent:
    given config: Configuration = Configuration.default.withDiscriminator("type")
    given encoderMyEvent: Encoder[MyEvent] = ConfiguredEncoder.derived[MyEvent]
    given decoderMyEvent: Decoder[MyEvent] = ConfiguredDecoder.derived[MyEvent]

    /*given encoderBar: Encoder[Bar] =
      ConfiguredEncoder.derived[Bar]*/

    // Encoder.forProduct3[Bar, String, Int, String]("type", "id", "msg")(bar => ("Bar", bar.id, bar.msg))
    /*given encoderBar: Encoder[Bar] = encoderMyEvent.contramap[Bar](e => {
      val event: MyEvent = e
      event
    })*/

    /*given decoderBar: Decoder[Bar] =
      ConfiguredDecoder.derived[Bar]*/
    // Decoder.forProduct3[Bar, String, Int, String]("type", "id", "msg")((_, id, msg) => Bar(id, msg))

    /*given encoderBaz: Encoder[Baz] =
      ConfiguredEncoder.derived[Baz]*/

    // Encoder.forProduct2[Baz, String, String]("type", "value")(baz => ("Baz", baz.value))
    /*given encoderBaz: Encoder[Baz] = encoderMyEvent.contramap[Baz](e => {
      val event: MyEvent = e
      event
    })*/

    /*given decoderBaz: Decoder[Baz] =
      ConfiguredDecoder.derived[Baz]*/
    // Decoder.forProduct2[Baz, String, String]("type", "value")((_, value) => Baz(value))

  sealed trait MyDeleted extends MyEvent
  final case class SubjectDeleted(subject: String) extends MyDeleted
  final case class VersionDeleted(subject: String, version: Int) extends MyDeleted
