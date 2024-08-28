package io.github.sergeiionin.contractsregistrator
package producer.contracts

import domain.events.contracts.{ContractDeletedEventKey, ContractDeletedEvent}
import producer.{EventsKafkaProducer, EventsProducer}
import cats.effect.{Async, Resource}
import cats.syntax.applicative.*
import fs2.kafka.{KafkaProducer, ProducerSettings, Serializer}

final class ContractDeleteEventsKafkaProducer[F[_] : Async](
                                                  override val topic: String,
                                                  override val kafkaProducer: KafkaProducer[F, ContractDeletedEventKey, ContractDeletedEvent]
                                                ) extends EventsKafkaProducer[F, ContractDeletedEventKey, ContractDeletedEvent]()

object ContractDeleteEventsKafkaProducer:
  import EventsKafkaProducer.producerSettings
  
  def make[F[_] : Async](
                          contractCreatedTopic: String,
                          bootstrapServers: String,
                        )(using Serializer[F, ContractDeletedEventKey],
                          Serializer[F, ContractDeletedEvent]): Resource[F, EventsProducer[F, ContractDeletedEventKey, ContractDeletedEvent]] =
    KafkaProducer[F].resource[ContractDeletedEventKey, ContractDeletedEvent](producerSettings(bootstrapServers))
      .map(kafkaProducer => ContractDeleteEventsKafkaProducer[F](contractCreatedTopic, kafkaProducer))
