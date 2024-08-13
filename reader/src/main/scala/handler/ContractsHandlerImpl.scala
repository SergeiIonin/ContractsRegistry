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

import org.typelevel.log4cats.Logger

class ContractsHandlerImpl[F[_] : Monad : Logger](repository: ContractsRepository[F],
                                                  gitClient: GitClient[F]) extends ContractsHandler[F]:
  private val logger = summon[Logger[F]]
  
  // todo it should be simplified
  def addContractPR(contract: Contract): F[Unit] =
    val ref = s"${contract.subject}-${System.currentTimeMillis()}" // fixme should be s"${contract.subject}-${contract.version}"
    for
      blobSha       <- gitClient.createBlob(contract.schema)
      latestSha     <- gitClient.getLatestSHA()
      _             <- gitClient.createRef(latestSha, ref)
      baseTreeSha   <- gitClient.getBaseTreeSha(latestSha)
      newTreeSha    <- gitClient.createNewTree(s"${contract.subject}.proto", baseTreeSha, blobSha)
      newCommitSha  <- gitClient.createCommit(newTreeSha, latestSha, s"Add contract ${contract.subject}")
      _             <- gitClient.updateBranchRef(ref, newCommitSha)
      _            <- logger.info(s"creating a PR for the contract ${contract.subject}")
      _             <- gitClient.createPR(s"Add contract ${contract.subject}", s"Add contract ${contract.subject}", ref)
    yield ()

  def deleteContractPR(subject: String, version: Int): F[Unit] =
    for
      latestSha    <- gitClient.getLatestSHA()
      fileName     = s"$subject.proto"
      branch       = s"delete-$subject-$version"
      _            <- gitClient.createRef(latestSha, branch)
      fileSha      <- gitClient.getContractSha(fileName)
      _            <- logger.info(s"deleting the file $fileName")
      newCommitSha <- gitClient.deleteContract(s"$fileName", fileSha, branch) // todo should the contract's name hold the versioin?
      _            <- gitClient.updateBranchRef(branch, newCommitSha)
      _            <- gitClient.createPR(s"Delete contract $fileName", s"Delete contract $fileName", branch)
    yield ()
  
  def addContract(contract: Contract): F[Unit] =
    for
      _ <- repository.save(contract)
      _ <- addContractPR(contract)
    yield ()
  
  def deleteContractVersion(subject: String, version: Int): F[Unit] =
    for
      _ <- repository.delete(subject, version)
      _ <- deleteContractPR(subject, version)
    yield ()
  
  // todo implement
  def deleteContract(subject: String): F[Unit] = ???