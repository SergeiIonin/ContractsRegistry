package io.github.sergeiionin.contractsregistrator
package domain.events.contracts

import domain.events.Key

import io.circe.{Decoder, Encoder}
import io.circe.derivation.{Configuration, ConfiguredEncoder, ConfiguredDecoder}

sealed trait ContractEventKey extends Key

final case class ContractCreateRequestedKey(
    subject: String,
    version: Int
) extends ContractEventKey

sealed trait ContractDeletedEventKey extends ContractEventKey

final case class ContractDeleteRequestedKey(
    subject: String
) extends ContractDeletedEventKey

final case class ContractVersionDeleteRequestedKey(
    subject: String,
    version: Int
) extends ContractDeletedEventKey
