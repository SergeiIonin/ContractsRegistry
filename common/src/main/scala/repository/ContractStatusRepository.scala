package io.github.sergeiionin.contractsregistrator
package repository

import cats.effect.Resource
import cats.effect.kernel.Async
import org.typelevel.log4cats.Logger
import skunk.Session

trait ContractStatusRepository[F[_]]:
  def updateIsMerged(subject: String, version: Int): F[Unit]

object ContractStatusRepository:
  def make[F[_] : Async : Logger](session: Resource[F, Session[F]]): Resource[F, ContractStatusRepository[F]] =
    Resource.pure[F, ContractStatusRepository[F]](ContractStatusRepositoryPostgresImpl[F](session))
