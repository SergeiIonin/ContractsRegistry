package io.github.sergeiionin.contractsregistrator
package producer.contracts

import domain.events.contracts.{ContractDeletedEventKey, ContractDeletedEvent}
import producer.{KafkaEventsProducer, EventsProducer}
import cats.effect.{Async, Resource}
import cats.syntax.applicative.*
import fs2.kafka.{KafkaProducer, ProducerSettings, Serializer}

final class ContractDeleteKafkaEventsProducer[F[_] : Async](
                                                  override val topic: String,
                                                  override val kafkaProducer: KafkaProducer[F, ContractDeletedEventKey, ContractDeletedEvent]
                                                ) extends KafkaEventsProducer[F, ContractDeletedEventKey, ContractDeletedEvent]()

object ContractDeleteKafkaEventsProducer:
  import KafkaEventsProducer.producerSettings
  
  def make[F[_] : Async](
                          contractCreatedTopic: String,
                          bootstrapServers: String,
                        )(using Serializer[F, ContractDeletedEventKey],
                          Serializer[F, ContractDeletedEvent]): Resource[F, EventsProducer[F, ContractDeletedEventKey, ContractDeletedEvent]] =
    KafkaProducer[F].resource[ContractDeletedEventKey, ContractDeletedEvent](producerSettings(bootstrapServers))
      .map(kafkaProducer => ContractDeleteKafkaEventsProducer[F](contractCreatedTopic, kafkaProducer))
