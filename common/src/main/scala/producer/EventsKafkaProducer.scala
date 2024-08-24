package io.github.sergeiionin.contractsregistrator
package producer

import cats.effect.Async
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerSettings, Serializer}
import io.github.sergeiionin.contractsregistrator.domain.events.contracts.{ContractDeleteRequested, ContractDeleteRequestedKey}

abstract class EventsKafkaProducer[F[_] : Async, K, V]() extends EventsProducer[F, K, V]:
  def kafkaProducer: KafkaProducer[F, K, V]
  
  override def produce(key: K, value: V): F[Unit] =
    kafkaProducer.produceOne(ProducerRecord(this.topic, key, value)).flatten.void

object EventsKafkaProducer:  
  def producerSettings[F[_], K, V](bootstrapServers: String)(using Serializer[F, K], Serializer[F, V]): ProducerSettings[F, K, V] =
    ProducerSettings[F, K, V](
      Serializer.apply[F, K],
      Serializer.apply[F, V],
    ).withBootstrapServers(bootstrapServers)
