package io.github.sergeiionin.contractsregistrator
package producer

import cats.effect.{Async, Resource}
import fs2.kafka.KafkaProducer
import domain.events.prs.{PrClosed, PrClosedKey}

trait GitHubEventsProducer[F[_], K, V]:
  def topic: String
  def produce(key: K, value: V): F[Unit]

object GitHubEventsProducer:
  def makePRsProducer[F[_] : Async](
                                     topic: String, 
                                     kafkaProducer: KafkaProducer[F, PrClosedKey, PrClosed]
                                   ): Resource[F, GitHubEventsProducer[F, PrClosedKey, PrClosed]] =
    Resource.pure[F, GitHubEventsProducer[F, PrClosedKey, PrClosed]](GitHubPRsKafkaProducer[F](topic, kafkaProducer))