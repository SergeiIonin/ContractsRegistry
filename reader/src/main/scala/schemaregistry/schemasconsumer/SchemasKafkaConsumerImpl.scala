package io.github.sergeiionin.contractsregistrator
package schemaregistry
package schemasconsumer

import cats.data.NonEmptyList
import cats.effect.kernel.Async
import fs2.kafka.{CommittableConsumerRecord, ConsumerSettings, Deserializer, KafkaConsumer}
import io.circe.{parser, Error as circeError}

final class SchemasKafkaConsumerImpl[F[_] : Async](kafkaConsumer: KafkaConsumer[F, Bytes, Bytes]) extends SchemasConsumer[F]:
  override def subscribe(topics: NonEmptyList[String]): F[Unit] =
    kafkaConsumer.subscribe(topics)
  
  override def stream(): fs2.Stream[F, CommittableConsumerRecord[F, Bytes, Bytes]] =
    kafkaConsumer.stream
