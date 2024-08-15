package io.github.sergeiionin.contractsregistrator
package handler

import cats.effect.Concurrent
import cats.data.NonEmptyList
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.implicits.catsSyntaxFlatMapOps
import cats.syntax.applicative.*
import fs2.kafka.{CommittableConsumerRecord, KafkaConsumer}
import domain.Contract
import repository.ContractsRepository
import github.GitHubClient

import org.typelevel.log4cats.Logger

class ContractsHandlerImpl[F[_] : Concurrent : Logger](repository: ContractsRepository[F],
                                                  gitClient: GitHubClient[F]) extends ContractsHandler[F]:
  private val logger = summon[Logger[F]]
  
  def addContractPR(contract: Contract): F[Unit] =
    for
      latestSha    <- gitClient.getLatestSHA()
      branch       =  gitClient.getBranchName("add", contract.subject, contract.version)
      _            <- gitClient.createBranch(latestSha, branch)
      newCommitSha <- gitClient.addContract(contract, branch)
      _            <- gitClient.updateBranchRef(branch, newCommitSha)
      _            <- logger.info(s"creating a PR for the contract ${contract.subject}:${contract.version}")
      _            <- gitClient.createPR(s"Add contract ${contract.subject}_${contract.version}",
                            s"Add contract ${contract.subject}_${contract.version}", branch)
    yield ()

  def deleteContractPR(subject: String, version: Int): F[Unit] =
    for
      latestSha    <- gitClient.getLatestSHA()
      fileName     = gitClient.getFileName(subject, version)
      branch       = gitClient.getBranchName("delete", subject, version)
      _            <- gitClient.createBranch(latestSha, branch)
      fileSha      <- gitClient.getContractSha(fileName)
      newCommitSha <- gitClient.deleteContract(subject, version, fileSha, branch)
      _            <- gitClient.updateBranchRef(branch, newCommitSha)
      _            <- logger.info(s"deleting the file $fileName")
      _            <- gitClient.createPR(s"Delete contract $fileName",
                            s"Delete contract $fileName", branch)
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
  
  // it's on purpose that we delete the each contract's versions as a separate PR
  def deleteContract(subject: String): F[Unit] =
    for
     versions <- repository.getAllVersionsForSubject(subject)
     _              <- versions
                        .parEvalMapUnordered(10)(version =>
                          deleteContractVersion(subject, version)
                        )
                        .compile
                        .drain
     
    yield ()  