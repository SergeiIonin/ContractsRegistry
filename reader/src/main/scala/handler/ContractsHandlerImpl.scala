package io.github.sergeiionin.contractsregistrator
package handler

import cats.Monad
import cats.data.NonEmptyList
import cats.implicits.catsSyntaxFlatMapOps
import cats.syntax.applicative.*
import fs2.kafka.{CommittableConsumerRecord, KafkaConsumer}
import repository.ContractsRepository

class ContractsHandlerImpl[F[_] : Monad](repository: ContractsRepository[F]) extends ContractsHandler[F]:
  def handle(contract: Contract): F[Unit] = repository.save(contract)
  
/*  def commitSync(offsets: Map[TopicPartition, OffsetAndMetadata]): F[Unit] = consumer.commitSync(offsets)

  def stopConsuming(): F[Unit] = consumer.stopConsuming

  def terminateConsumer(): F[Unit] = consumer.terminate
*/

