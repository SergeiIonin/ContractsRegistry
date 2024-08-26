package io.github.sergeiionin.contractsregistrator
package domain.events.contracts

import domain.events.{Event, Key}
import io.circe.{Decoder, Encoder}

final case class ContractDeleteRequestedKey(subject: String, version: Option[Int]) extends Key derives Encoder, Decoder
final case class ContractDeleteRequested(subject: String, version: Option[Int], deleteSubject: Boolean = false) extends Event derives Encoder, Decoder

final case class ContractCreateRequestedKey(subject: String, version: Int) extends Key derives Encoder, Decoder
final case class ContractCreateRequested(subject: String, version: Int) extends Event derives Encoder, Decoder

