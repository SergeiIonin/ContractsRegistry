package io.github.sergeiionin.contractsregistrator
package github

import cats.Monad
import cats.syntax.functor.*
import cats.syntax.flatMap.*
import cats.effect.{Concurrent, Resource}
import domain.{Contract, ContractPullRequest}
import service.ContractService
import github.GitHubClient
import org.typelevel.log4cats.Logger

final class GitHubServiceImpl[F[_] : Concurrent : Logger](
                                              gitHubClient: GitHubClient[F],
                                              contractService: ContractService[F]
                                            ) extends GitHubService[F]:
  private val logger = summon[Logger[F]]

  private def addContractPR(contract: Contract): F[Unit] =
    for
      latestSha    <- gitHubClient.getLatestSHA()
      branch       =  gitHubClient.getBranchName("add", contract.subject, contract.version)
      _            <- gitHubClient.createBranch(latestSha, branch)
      newCommitSha <- gitHubClient.addContract(contract, branch)
      _            <- gitHubClient.updateBranchRef(branch, newCommitSha)
      contractPR   =  ContractPullRequest.fromContract(contract)
      _            <- logger.info(s"creating a PR for the contract ${contract.subject}:${contract.version}")
      _            <- gitHubClient.createPR(contractPR.getTitle(), contractPR.getBody(), branch)
    yield ()

  private def deleteContractPR(subject: String, version: Int): F[Unit] =
    for
      latestSha    <- gitHubClient.getLatestSHA()
      fileName     =  gitHubClient.getFileName(subject, version)
      branch       =  gitHubClient.getBranchName("delete", subject, version)
      _            <- gitHubClient.createBranch(latestSha, branch)
      fileSha      <- gitHubClient.getContractSha(fileName)
      newCommitSha <- gitHubClient.deleteContract(subject, version, fileSha, branch)
      _            <- gitHubClient.updateBranchRef(branch, newCommitSha)
      contractPR   =  ContractPullRequest(subject, version, isDeleted = true)
      _            <- logger.info(s"deleting the file $fileName")
      _            <- gitHubClient.createPR(contractPR.getTitle(), contractPR.getBody(), branch)
    yield ()

  override def addContract(contract: Contract): F[Unit] =
    addContractPR(contract)
  
  override def deleteContractVersion(subject: String, version: Int): F[Unit] =
    deleteContractPR(subject, version)

  override def deleteContract(subject: String): F[Unit] =
    for
      versions <- contractService.getContractVersions(subject)
      _        <- versions
                    .parEvalMapUnordered(10)(version =>
                      deleteContractVersion(subject, version)
                    )
                    .compile
                    .drain
    yield ()
