package io.github.sergeiionin.contractsregistrator
package producer

import cats.effect.Async
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import fs2.kafka.{KafkaProducer, ProducerRecord}
import producer.events.prs.{PrClosedKey, PrClosed}

final class GitHubPRsKafkaProducer[F[_] : Async](
                                                  override val topic: String,
                                                  kafkaProducer: KafkaProducer[F, PrClosedKey, PrClosed]
                                                ) extends GitHubEventsKafkaProducer[F, PrClosedKey, PrClosed](kafkaProducer)
    
