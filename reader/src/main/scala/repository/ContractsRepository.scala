package io.github.sergeiionin.contractsregistrator
package repository

import domain.Contract

import skunk.Session
import cats.effect.Resource
import cats.effect.kernel.Async

trait ContractsRepository[F[_]]:
  def save(contract: Contract): F[Unit]
  def get(name: String): F[Option[Contract]]
  def getAll(): F[fs2.Stream[F, Contract]]
  def delete(name: String): F[Unit]
  
object ContractsRepository:
  def make[F[_] : Async](session: Session[F]): Resource[F, ContractsRepository[F]] =
    Resource.pure[F, ContractsRepository[F]](ContractsRepositoryPostgresImpl[F](session))