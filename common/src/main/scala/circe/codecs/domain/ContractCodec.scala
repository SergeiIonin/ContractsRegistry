package io.github.sergeiionin.contractsregistrator
package circe.codecs.domain

import domain.Contract

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

object ContractCodec:
  given contractEncoder: Encoder[Contract] = deriveEncoder[Contract]
  given contractDecoder: Decoder[Contract] = deriveDecoder[Contract]
