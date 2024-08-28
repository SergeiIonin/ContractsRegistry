package io.github.sergeiionin.contractsregistrator
package producer

import cats.Applicative
import cats.syntax.applicative.*
import producer.EventsProducer
import domain.events.contracts.{ContractDeletedEventKey, ContractDeletedEvent}

class TestContractsEventsProducer[F[_] : Applicative] extends EventsProducer[F, ContractDeletedEventKey, ContractDeletedEvent]:
  override val topic: String = "events_contracts_deleted"
  override def produce(key: ContractDeletedEventKey, value: ContractDeletedEvent): F[Unit] = ().pure[F]