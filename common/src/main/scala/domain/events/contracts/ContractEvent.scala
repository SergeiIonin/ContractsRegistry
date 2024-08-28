package io.github.sergeiionin.contractsregistrator
package domain.events.contracts

import domain.Contract
import domain.events.Event

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{Configuration, ConfiguredDecoder, ConfiguredEncoder}

sealed trait ContractEvent extends Event

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
