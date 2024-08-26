package io.github.sergeiionin.contractsregistrator
package producer.contracts

import domain.events.contracts.{ContractDeleteRequested, ContractDeleteRequestedKey}
import producer.{EventsKafkaProducer, EventsProducer}
import cats.effect.{Async, Resource}
import cats.syntax.applicative.*
import fs2.kafka.{KafkaProducer, ProducerSettings, Serializer}

final class ContractDeleteEventsKafkaProducer[F[_] : Async](
                                                  override val topic: String,
                                                  override val kafkaProducer: KafkaProducer[F, ContractDeleteRequestedKey, ContractDeleteRequested]
                                                ) extends EventsKafkaProducer[F, ContractDeleteRequestedKey, ContractDeleteRequested]()

object ContractDeleteEventsKafkaProducer:
  import EventsKafkaProducer.producerSettings
  
  def make[F[_] : Async](
                          contractCreatedTopic: String,
                          bootstrapServers: String,
                        )(using Serializer[F, ContractDeleteRequestedKey],
                          Serializer[F, ContractDeleteRequested]): Resource[F, EventsProducer[F, ContractDeleteRequestedKey, ContractDeleteRequested]] =
    KafkaProducer[F].resource[ContractDeleteRequestedKey, ContractDeleteRequested](producerSettings(bootstrapServers))
      .map(kafkaProducer => ContractDeleteEventsKafkaProducer[F](contractCreatedTopic, kafkaProducer))
