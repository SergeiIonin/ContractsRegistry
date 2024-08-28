package io.github.sergeiionin.contractsregistrator
package domain.events.contracts

import domain.events.Key

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{Configuration, ConfiguredEncoder, ConfiguredDecoder}

sealed trait ContractEventKey extends Key
object ContractEventKey:
  given config: Configuration = Configuration.default.withDiscriminator("type")

  given encoderContractEventKey: Encoder[ContractEventKey] = ConfiguredEncoder.derived[ContractEventKey]
  given decoderContractEventKey: Decoder[ContractEventKey] = ConfiguredDecoder.derived[ContractEventKey]

  given encoderCreateRequestedKey: Encoder[ContractCreateRequestedKey] =
    ConfiguredEncoder.derived[ContractCreateRequestedKey]
  given decoderCreateRequestedKey: Decoder[ContractCreateRequestedKey] =
    ConfiguredDecoder.derived[ContractCreateRequestedKey]

  given encoderDeleteEventKey: Encoder[ContractDeletedEventKey] =
    ConfiguredEncoder.derived[ContractDeletedEventKey]
  given decoderDeleteEventKey: Decoder[ContractDeletedEventKey] =
    ConfiguredDecoder.derived[ContractDeletedEventKey]

final case class ContractCreateRequestedKey(
                                             subject: String,
                                             version: Int
                                           ) extends ContractEventKey

sealed trait ContractDeletedEventKey extends ContractEventKey

final case class ContractDeleteRequestedKey(
                                             subject: String,
                                           ) extends ContractDeletedEventKey

final case class ContractVersionDeleteRequestedKey(
                                                    subject: String,
                                                    version: Int
                                                  ) extends ContractDeletedEventKey
