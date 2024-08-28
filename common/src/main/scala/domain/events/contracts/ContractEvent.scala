package io.github.sergeiionin.contractsregistrator
package domain.events.contracts

import domain.Contract
import domain.events.Event

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{Configuration, ConfiguredDecoder, ConfiguredEncoder}

sealed trait ContractEvent extends Event
object ContractEvent:
  given config: Configuration = Configuration.default.withDiscriminator("type")
  
  given encoderContractEvent: Encoder[ContractEvent] = ConfiguredEncoder.derived[ContractEvent]
  given decoderContractEvent: Decoder[ContractEvent] = ConfiguredDecoder.derived[ContractEvent]
  
  given encoderCreateRequested: Encoder[ContractCreateRequested] =
    ConfiguredEncoder.derived[ContractCreateRequested]
  given decoderCreateRequested: Decoder[ContractCreateRequested] =
    ConfiguredDecoder.derived[ContractCreateRequested]

  given encoderDeleteEvent: Encoder[ContractDeletedEvent] =
    ConfiguredEncoder.derived[ContractDeletedEvent]
  given decoderDeleteEvent: Decoder[ContractDeletedEvent] =
    ConfiguredDecoder.derived[ContractDeletedEvent]  

final case class ContractCreateRequested(
                                          contract: Contract
                                        ) extends ContractEvent

sealed trait ContractDeletedEvent extends ContractEvent

final case class ContractDeleteRequested(
                                          subject: String,
                                        ) extends ContractDeletedEvent

final case class ContractVersionDeleteRequested(
                                                 subject: String,
                                                 version: Int,
                                               ) extends ContractDeletedEvent
