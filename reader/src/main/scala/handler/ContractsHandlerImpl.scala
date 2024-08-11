package io.github.sergeiionin.contractsregistrator
package handler

import cats.Monad
import cats.data.NonEmptyList
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.implicits.catsSyntaxFlatMapOps
import cats.syntax.applicative.*
import fs2.kafka.{CommittableConsumerRecord, KafkaConsumer}
import domain.Contract
import repository.ContractsRepository

import github.GitClient

class ContractsHandlerImpl[F[_] : Monad](repository: ContractsRepository[F],
                                         gitClient: GitClient[F]
                                        ) extends ContractsHandler[F]:
  def createPR(contract: Contract): F[Unit] =
    val ref = s"${contract.subject}-${System.currentTimeMillis()}"
    for
      blobSha       <- gitClient.createBlob(contract.schema)
      latestSha     <- gitClient.getLatestSHA()
      _             <- gitClient.createRef(latestSha, ref)
      baseTreeSha   <- gitClient.getBaseTreeSha(latestSha)
      newTreeSha    <- gitClient.createNewTree(s"${contract.subject}", baseTreeSha, blobSha)
      newCommitSha  <- gitClient.createCommit(newTreeSha, latestSha, s"Add contract ${contract.subject}")
      _             <- gitClient.updateBranchRef(ref, newCommitSha)
      _             <- gitClient.createPR(s"Add contract ${contract.subject}", s"Add contract ${contract.subject}", ref)
    yield ()

  def handle(contract: Contract): F[Unit] =
    for
      _ <- repository.save(contract)
      _ <- createPR(contract)
    yield ()
