package io.github.sergeiionin.contractsregistrator
package github

import cats.effect.{Concurrent, Resource}
import domain.Contract
import github.GitHubClient
import service.ContractService
import org.typelevel.log4cats.Logger

trait GitHubService[F[_]]:
  def addContract(contract: Contract): F[Unit]

  def deleteContractVersion(subject: String, version: Int): F[Unit]

  def deleteContract(subject: String): F[Unit]

object GitHubService:
  def make[F[_] : Concurrent : Logger](
                              service: ContractService[F],
                              gitClient: GitHubClient[F]
                             ): Resource[F, GitHubService[F]] =
    Resource.pure[F, GitHubService[F]](GitHubServiceImpl[F](gitClient, service))
