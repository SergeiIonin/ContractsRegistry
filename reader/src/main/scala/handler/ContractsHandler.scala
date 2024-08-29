package io.github.sergeiionin.contractsregistrator
package handler

import cats.effect.{Resource, Concurrent}
import domain.Contract
import repository.ContractsRepository
import github.GitHubClient

import org.typelevel.log4cats.Logger
// fixme move test functionality to GitHubServiceSpec and rm it
trait ContractsHandler[F[_]]:
  def addContract(contract: Contract): F[Unit]
  // set isMerged for contract to true
  def updateIsMergedStatus(subject: String, version: Int): F[Unit]
  def deleteContractVersion(subject: String, version: Int): F[Unit]
  def deleteContract(subject: String): F[Unit]

object ContractsHandler:
  def make[F[_] : Concurrent](repository: ContractsRepository[F],
                          gitClient: GitHubClient[F]
                        )(using Logger[F]): Resource[F, ContractsHandler[F]] =
    Resource.pure[F, ContractsHandler[F]](ContractsHandlerImpl[F](repository, gitClient))