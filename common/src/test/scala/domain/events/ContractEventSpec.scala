package io.github.sergeiionin.contractsregistrator
package domain
package events

import domain.events.contracts.*
import domain.events.contracts.ContractEvent.given
import domain.events.contracts.ContractEventKey.given
import domain.{Contract, SchemaType}

import io.circe.parser.decode
import io.circe.syntax.*
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ContractEventSpec extends AnyWordSpec with Matchers:
  "ContractEventKey" should {
    "encode and decode ContractCreateRequestedKey correctly" in {
      val key: ContractCreateRequestedKey = ContractCreateRequestedKey("subject1", 1)
      val json = key.asJson.noSpaces
      val decodedKey = decode[ContractCreateRequestedKey](json)
      decodedKey shouldBe Right(key)
    }

    "encode and decode ContractDeleteRequestedKey correctly" in {
      val key: ContractDeletedEventKey = ContractDeleteRequestedKey("subject2")
      val json = key.asJson.noSpaces
      val decodedKey = decode[ContractDeletedEventKey](json)
      decodedKey shouldBe Right(key)
    }

    "encode and decode ContractVersionDeleteRequestedKey correctly" in {
      val key: ContractDeletedEventKey = ContractVersionDeleteRequestedKey("subject3", 2)
      val json = key.asJson.noSpaces
      val decodedKey = decode[ContractDeletedEventKey](json)
      decodedKey shouldBe Right(key)
    }
  }

  "ContractEvent" should {
    "encode and decode ContractCreateRequested correctly" in {
      val event: ContractCreateRequested = ContractCreateRequested(Contract("subject1", 1, 1, "schema1", SchemaType.PROTOBUF, false, None))
      val json = event.asJson.noSpaces
      val decodedEvent = decode[ContractCreateRequested](json)
      decodedEvent shouldBe Right(event)
    }

    "encode and decode ContractDeleteRequested correctly" in {
      val event: ContractDeletedEvent = ContractDeleteRequested("subject2")
      val json = event.asJson.noSpaces
      val decodedEvent = decode[ContractDeletedEvent](json)
      decodedEvent shouldBe Right(event)
    }

    "encode and decode ContractVersionDeleteRequested correctly" in {
      val event: ContractDeletedEvent = ContractVersionDeleteRequested("subject3", 2)
      val json = event.asJson.noSpaces
      val decodedEvent = decode[ContractDeletedEvent](json)
      decodedEvent shouldBe Right(event)
    }
  }
  