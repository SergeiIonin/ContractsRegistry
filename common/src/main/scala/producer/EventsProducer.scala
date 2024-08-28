package io.github.sergeiionin.contractsregistrator
package producer

trait EventsProducer[F[_], K, V]:
  def topic: String
  def produce(key: K, value: V): F[Unit]
