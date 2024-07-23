package io.github.sergeiionin.contractsregistrator
package handler

import repository.ContractsRepository
import cats.effect.Resource

trait ContractsHandler[F[_]]:
  def handle(contract: Contract): F[Unit]

object ContractsHandler:
  def make[F[_]](repository: ContractsRepository[F]): Resource[F, ContractsHandlerImpl[F]] =
    Resource.pure[F](ContractsHandlerImpl[F](repository))