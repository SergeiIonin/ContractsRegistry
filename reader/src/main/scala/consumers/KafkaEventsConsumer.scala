package io.github.sergeiionin.contractsregistrator
package consumers

import fs2.kafka.KafkaConsumer
import cats.data.NonEmptyList

abstract class KafkaEventsConsumer[F[_], K, V](kafkaConsumer: KafkaConsumer[F, K, V]) extends Consumer[F]:
  override def subscribe(topics: NonEmptyList[String]): F[Unit] =
    kafkaConsumer.subscribe(topics)
