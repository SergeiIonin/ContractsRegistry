package io.github.sergeiionin.contractsregistrator
package producer.prs

import cats.syntax.applicative.*
import domain.events.contracts.{ContractDeleteRequested, ContractDeleteRequestedKey}
import domain.events.prs.{PrClosed, PrClosedKey}
import producer.{EventsKafkaProducer, EventsProducer}

import cats.effect.{Async, Resource}
import fs2.kafka.{KafkaProducer, ProducerSettings, Serializer}
import io.circe.syntax.*

final class PrEventsKafkaProducer[F[_] : Async](
                                                  override val topic: String,
                                                  override val kafkaProducer: KafkaProducer[F, PrClosedKey, PrClosed]
                                                ) extends EventsKafkaProducer[F, PrClosedKey, PrClosed]()

object PrEventsKafkaProducer:
  import EventsKafkaProducer.producerSettings
  
  def make[F[_] : Async](
                          topic: String,
                          bootstrapServers: String,
                        ): Resource[F, EventsProducer[F, PrClosedKey, PrClosed]] =

    given Serializer[F, PrClosedKey] = Serializer.lift(key => key.asJson.noSpaces.getBytes.pure[F])
    given Serializer[F, PrClosed] = Serializer.lift(event => event.asJson.noSpaces.getBytes.pure[F])

    KafkaProducer[F].resource[PrClosedKey, PrClosed](producerSettings(bootstrapServers))
      .map(kafkaProducer => PrEventsKafkaProducer[F](topic, kafkaProducer))    
