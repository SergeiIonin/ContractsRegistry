package io.github.sergeiionin.contractsregistrator
package handler

import cats.effect.Resource
import cats.Monad
import domain.Contract
import repository.ContractsRepository
import github.GitClient

import org.typelevel.log4cats.Logger

trait ContractsHandler[F[_]]:
  def addContract(contract: Contract): F[Unit]
  def deleteContract(subject: String, version: Int): F[Unit]

object ContractsHandler:
  def make[F[_] : Monad](repository: ContractsRepository[F],
                          gitClient: GitClient[F]
                        )(using Logger[F]): Resource[F, ContractsHandler[F]] =
    Resource.pure[F, ContractsHandler[F]](ContractsHandlerImpl[F](repository, gitClient))