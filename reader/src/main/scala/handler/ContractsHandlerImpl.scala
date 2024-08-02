package io.github.sergeiionin.contractsregistrator
package handler

import cats.Monad
import cats.data.NonEmptyList
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.implicits.catsSyntaxFlatMapOps
import cats.syntax.applicative.*
import fs2.kafka.{CommittableConsumerRecord, KafkaConsumer}
import repository.ContractsRepository
import handler.protos.ProtosHandler
import domain.Contract

class ContractsHandlerImpl[F[_] : Monad](repository: ContractsRepository[F],
                                         protos: ProtosHandler[F],
                                        ) extends ContractsHandler[F]:
  def handle(contract: Contract): F[Unit] = 
    for
      _ <- repository.save(contract)
      _ <- protos.saveProto(contract.subject, contract.schema)
    yield ()
