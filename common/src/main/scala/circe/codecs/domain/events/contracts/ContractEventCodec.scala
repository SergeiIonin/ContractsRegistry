package io.github.sergeiionin.contractsregistrator
package circe.codecs.domain.events.contracts

import domain.events.contracts.*
import circe.codecs.domain.ContractCodec.given
import io.circe.derivation.{Configuration, ConfiguredDecoder, ConfiguredEncoder}
import io.circe.syntax.*
import io.circe.{Decoder, Encoder, Json}

object ContractEventCodec:
  given config: Configuration = Configuration.default.withDiscriminator("type")

  given encoderContractEvent: Encoder[ContractEvent] = ConfiguredEncoder.derived[ContractEvent]
  given decoderContractEvent: Decoder[ContractEvent] = ConfiguredDecoder.derived[ContractEvent]

  given encoderCreateRequested: Encoder[ContractCreateRequested] =
    (c: ContractCreateRequested) =>
      Json.obj(
        "type" -> Json.fromString("ContractCreateRequested"),
        "contract" -> c.contract.asJson
      )
  given decoderCreateRequested: Decoder[ContractCreateRequested] =
    ConfiguredDecoder.derived[ContractCreateRequested]

  given encoderDeleteEvent: Encoder[ContractDeletedEvent] =
    ConfiguredEncoder.derived[ContractDeletedEvent]
  given decoderDeleteEvent: Decoder[ContractDeletedEvent] =
    ConfiguredDecoder.derived[ContractDeletedEvent]

  given encoderContractDeleteRequested: Encoder[ContractDeleteRequested] =
    (c: ContractDeleteRequested) =>
      Json.obj(
        "type" -> Json.fromString("ContractDeleteRequested"),
        "subject" -> Json.fromString(c.subject),
        "versions" -> Json.fromValues(c.versions.map(Json.fromInt))
      )
  given decoderContractDeleteRequested: Decoder[ContractDeleteRequested] =
    ConfiguredDecoder.derived[ContractDeleteRequested]

  given encoderContractVersionDeleteRequested: Encoder[ContractVersionDeleteRequested] =
    (c: ContractVersionDeleteRequested) =>
      Json.obj(
        "type" -> Json.fromString("ContractVersionDeleteRequested"),
        "subject" -> Json.fromString(c.subject),
        "version" -> Json.fromInt(c.version)
      )
  given decoderContractVersionDeleteRequested: Decoder[ContractVersionDeleteRequested] =
    ConfiguredDecoder.derived[ContractVersionDeleteRequested]
