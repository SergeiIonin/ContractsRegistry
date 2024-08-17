package io.github.sergeiionin.contractsregistrator
package producer

import cats.effect.Async
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import fs2.kafka.{KafkaProducer, ProducerRecord}

abstract class GitHubEventsKafkaProducer[F[_] : Async, K, V](kafkaProducer: KafkaProducer[F, K, V]) extends GitHubEventsProducer[F, K, V]:
  override def produce(key: K, value: V): F[Unit] =
    kafkaProducer.produceOne(ProducerRecord(this.topic, key, value)).flatten.void
    
