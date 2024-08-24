package io.github.sergeiionin.contractsregistrator
package producer.contracts

import domain.events.contracts.{ContractDeleteRequested, ContractDeleteRequestedKey}
import domain.events.prs.{PrClosed, PrClosedKey}
import producer.{EventsKafkaProducer, EventsProducer}
import io.circe.syntax.*
import cats.syntax.applicative.*
import cats.effect.{Async, IO, Resource}
import fs2.kafka.{KafkaProducer, ProducerSettings, Serializer}

final class ContractEventsKafkaProducer[F[_] : Async](
                                                  override val topic: String,
                                                  override val kafkaProducer: KafkaProducer[F, ContractDeleteRequestedKey, ContractDeleteRequested]
                                                ) extends EventsKafkaProducer[F, ContractDeleteRequestedKey, ContractDeleteRequested]()

object ContractEventsKafkaProducer:
  import EventsKafkaProducer.producerSettings
  
  def make[F[_] : Async](
                          topic: String,
                          bootstrapServers: String,
                        ): Resource[F, EventsProducer[F, ContractDeleteRequestedKey, ContractDeleteRequested]] =

    given Serializer[F, ContractDeleteRequestedKey] = Serializer.lift(key => key.asJson.noSpaces.getBytes.pure[F])
    given Serializer[F, ContractDeleteRequested] = Serializer.lift(event => event.asJson.noSpaces.getBytes.pure[F])

    KafkaProducer[F].resource[ContractDeleteRequestedKey, ContractDeleteRequested](producerSettings(bootstrapServers))
      .map(kafkaProducer => ContractEventsKafkaProducer[F](topic, kafkaProducer))
