package io.github.sergeiionin.contractsregistrator
package producer

import cats.effect.{Async, Resource}
import fs2.kafka.KafkaProducer
import domain.events.prs.{PrClosed, PrClosedKey}

trait EventsProducer[F[_], K, V]:
  def topic: String
  def produce(key: K, value: V): F[Unit]
