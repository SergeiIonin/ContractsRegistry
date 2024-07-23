package io.github.sergeiionin.contractsregistrator
package repository

import domain.Contract
import skunk.Session
import cats.effect.Resource

trait ContractsRepository[F[_]]:
  def save(contract: Contract): F[Unit]
  def get(name: String): F[Option[Contract]]
  def getAll(): F[fs2.Stream[F, Contract]]
  def delete(name: String): F[Unit]
  
object ContractsRepository:
  def make[F[_]](session: Session[F]): Resource[F, ContractsRepositoryPostgresImpl[F]] =
    Resource.eval[F](ContractsRepositoryPostgresImpl[F](session))