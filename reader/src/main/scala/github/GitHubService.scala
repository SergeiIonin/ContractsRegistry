package io.github.sergeiionin.contractsregistrator
package github

import cats.effect.{Concurrent, Resource}
import domain.Contract
import github.GitHubClient

import cats.Parallel
import org.typelevel.log4cats.Logger

trait GitHubService[F[_]]:
  def addContract(contract: Contract): F[Unit]

  def deleteContractVersion(subject: String, version: Int): F[Unit]

  def deleteContract(subject: String, versions: List[Int]): F[Unit]

object GitHubService:
  def make[F[_] : Concurrent : Parallel : Logger](
                              gitClient: GitHubClient[F]
                             ): Resource[F, GitHubService[F]] =
    Resource.pure[F, GitHubService[F]](GitHubServiceImpl[F](gitClient))
