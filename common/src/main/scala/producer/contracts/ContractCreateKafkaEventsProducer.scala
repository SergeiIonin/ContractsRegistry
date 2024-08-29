package io.github.sergeiionin.contractsregistrator
package producer.contracts

import domain.events.contracts.{ContractCreateRequestedKey, ContractCreateRequested}
import producer.{KafkaEventsProducer, EventsProducer}
import cats.effect.{Async, Resource}
import cats.syntax.applicative.*
import fs2.kafka.{KafkaProducer, ProducerSettings, Serializer}

final class ContractCreateKafkaEventsProducer[F[_] : Async](
                                                  override val topic: String,
                                                  override val kafkaProducer: KafkaProducer[F, ContractCreateRequestedKey, ContractCreateRequested]
                                                ) extends KafkaEventsProducer[F, ContractCreateRequestedKey, ContractCreateRequested]()

object ContractCreateKafkaEventsProducer:
  import KafkaEventsProducer.producerSettings
  
  def make[F[_] : Async](
                          contractCreatedTopic: String,
                          bootstrapServers: String,
                        )(using Serializer[F, ContractCreateRequestedKey],
                          Serializer[F, ContractCreateRequested]): Resource[F, EventsProducer[F, ContractCreateRequestedKey, ContractCreateRequested]] =
    KafkaProducer[F].resource[ContractCreateRequestedKey, ContractCreateRequested](producerSettings(bootstrapServers))
      .map(kafkaProducer => ContractCreateKafkaEventsProducer[F](contractCreatedTopic, kafkaProducer))
