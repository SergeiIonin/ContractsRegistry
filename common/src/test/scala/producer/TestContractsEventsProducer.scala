package io.github.sergeiionin.contractsregistrator
package producer

import cats.Applicative
import cats.syntax.applicative.*
import producer.EventsProducer
import domain.events.contracts.{ContractEventKey, ContractEvent}

abstract class TestContractsEventsProducer[
    F[_]: Applicative,
    K <: ContractEventKey,
    V <: ContractEvent]
    extends EventsProducer[F, K, V]:
  override val topic: String = "events_contracts"
  override def produce(key: K, value: V): F[Unit] = ().pure[F]
