package io.github.sergeiionin.contractsregistrator
package domain.events.contracts

import domain.events.{Event, Key}
import io.circe.{Decoder, Encoder}

final case class ContractDeleteRequested(subject: String, version: Option[Int], deleteSubject: Boolean = false) extends Event derives Encoder, Decoder
final case class ContractDeleteRequestedKey(subject: String) extends Key derives Encoder, Decoder

