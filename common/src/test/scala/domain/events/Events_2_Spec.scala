package io.github.sergeiionin.contractsregistrator
package domain.events

import domain.events.contracts.*
import domain.{Contract, SchemaType}

import io.circe.derivation.{Configuration, ConfiguredDecoder, ConfiguredEncoder}
import io.circe.parser.decode
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class Events_2_Spec extends AnyWordSpec with Matchers:
  import domain.events.Events_2_Spec.MyEvent.given
  import domain.events.Events_2_Spec.*
  "ContractEventKey" should {
    "encode and decode Bar" in {
      val bar = Bar(1, "hello")
      val baz = Baz("world")
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

      /*      given encoderBar: Encoder[Bar] = summon[Encoder[Bar]]
      given encoderBaz: Encoder[Baz] = summon[Encoder[Baz]]

      given decoderBar: Decoder[Bar] = summon[Decoder[Bar]]
      given decoderBaz: Decoder[Baz] = summon[Decoder[Baz]]*/

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

      val decodedBarEvent = decode[MyEvent](jsonBar)
      val decodedBazEvent = decode[MyEvent](jsonBaz)
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

object Events_2_Spec:
  sealed trait MyEvent
  // Define case classes extending the sealed trait
  final case class Bar(id: Int, msg: String) extends MyEvent // `type`: String = "Bar"
  final case class Baz(value: String) extends MyEvent
  // Circe configuration with a discriminator
  object MyEvent:
    given config: Configuration = Configuration.default.withDiscriminator("type")
    given encoderMyEvent: Encoder[MyEvent] = ConfiguredEncoder.derived[MyEvent]
    given decoderMyEvent: Decoder[MyEvent] = ConfiguredDecoder.derived[MyEvent]

    given encoderBar: Encoder[Bar] =
      (bar: Bar) =>
        Json.obj(
          "type" -> Json.fromString("Bar"),
          "id" -> Json.fromInt(bar.id),
          "msg" -> Json.fromString(bar.msg)
        )
    given decoderBar: Decoder[Bar] =
      ConfiguredDecoder.derived[Bar]

    given encoderBaz: Encoder[Baz] =
      (baz: Baz) =>
        Json.obj(
          "type" -> Json.fromString("Baz"),
          "value" -> Json.fromString(baz.value)
        )
    given decoderBaz: Decoder[Baz] =
      ConfiguredDecoder.derived[Baz]

  // Encoder.forProduct3[Bar, String, Int, String]("type", "id", "msg")(bar => ("Bar", bar.id, bar.msg))
  /*given encoderBar: Encoder[Bar] = encoderMyEvent.contramap[Bar](e => {
    val event: MyEvent = e
    event
  })*/

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
