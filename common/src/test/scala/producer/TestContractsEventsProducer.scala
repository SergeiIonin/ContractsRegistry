package io.github.sergeiionin.contractsregistrator
package producer

import cats.Applicative
import cats.syntax.applicative.*
import producer.EventsProducer
import domain.events.contracts.{ContractDeleteRequested, ContractDeleteRequestedKey}

class TestContractsEventsProducer[F[_] : Applicative] extends EventsProducer[F, ContractDeleteRequestedKey, ContractDeleteRequested]:
  override val topic: String = "events_contracts_deleted"
  override def produce(key: ContractDeleteRequestedKey, value: ContractDeleteRequested): F[Unit] = ().pure[F]