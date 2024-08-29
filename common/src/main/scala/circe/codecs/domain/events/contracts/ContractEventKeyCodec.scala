package io.github.sergeiionin.contractsregistrator
package circe.codecs.domain.events.contracts

import domain.events.contracts.*

import io.circe.derivation.{Configuration, ConfiguredDecoder, ConfiguredEncoder}
import io.circe.{Decoder, Encoder, Json}

object ContractEventKeyCodec:
  given config: Configuration = Configuration.default.withDiscriminator("type")

  given encoderContractEventKey: Encoder[ContractEventKey] = ConfiguredEncoder.derived[ContractEventKey]
  given decoderContractEventKey: Decoder[ContractEventKey] = ConfiguredDecoder.derived[ContractEventKey]

  given encoderCreateRequestedKey: Encoder[ContractCreateRequestedKey] =
    (c: ContractCreateRequestedKey) =>
      Json.obj(
        "type" -> Json.fromString("ContractCreateRequestedKey"),
        "subject" -> Json.fromString(c.subject),
        "version" -> Json.fromInt(c.version)
      )
  given decoderCreateRequestedKey: Decoder[ContractCreateRequestedKey] =
    ConfiguredDecoder.derived[ContractCreateRequestedKey]

  given encoderDeleteEventKey: Encoder[ContractDeletedEventKey] = ConfiguredEncoder.derived[ContractDeletedEventKey]
  given decoderDeleteEventKey: Decoder[ContractDeletedEventKey] = ConfiguredDecoder.derived[ContractDeletedEventKey]

  given encoderContractDeleteRequestedKey: Encoder[ContractDeleteRequestedKey] =
    (c: ContractDeleteRequestedKey) =>
      Json.obj(
        "type" -> Json.fromString("ContractDeleteRequestedKey"),
        "subject" -> Json.fromString(c.subject)
      )
  given decoderContractDeleteRequestedKey: Decoder[ContractDeleteRequestedKey] =
    ConfiguredDecoder.derived[ContractDeleteRequestedKey]

  given encoderContractVersionDeleteRequestedKey: Encoder[ContractVersionDeleteRequestedKey] =
    (c: ContractVersionDeleteRequestedKey) =>
      Json.obj(
        "type" -> Json.fromString("ContractVersionDeleteRequestedKey"),
        "subject" -> Json.fromString(c.subject),
        "version" -> Json.fromInt(c.version)
      )
  given decoderContractVersionDeleteRequestedKey: Decoder[ContractVersionDeleteRequestedKey] =
    ConfiguredDecoder.derived[ContractVersionDeleteRequestedKey]
