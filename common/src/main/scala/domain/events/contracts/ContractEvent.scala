package io.github.sergeiionin.contractsregistrator
package domain.events.contracts

import domain.Contract
import domain.events.{Event, Key}
import io.circe.{Decoder, Encoder}

sealed trait ContractEventKey extends Key
sealed trait ContractEvent extends Event

final case class ContractDeleteRequestedKey(
                                             subject: String,
                                             version: Option[Int]
                                           ) extends ContractEventKey derives Encoder, Decoder
final case class ContractDeleteRequested(
                                          subject: String,
                                          version: Option[Int],
                                          deleteSubject: Boolean = false
                                        ) extends ContractEvent derives Encoder, Decoder

final case class ContractCreateRequestedKey(
                                             subject: String,
                                             version: Int
                                           ) extends ContractEventKey derives Encoder, Decoder
final case class ContractCreateRequested(
                                          contract: Contract
                                        ) extends ContractEvent derives Encoder, Decoder

