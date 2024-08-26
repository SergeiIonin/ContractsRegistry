package io.github.sergeiionin.contractsregistrator
package repository

import domain.{Contract, SubjectAndVersion}

import skunk.Session
import cats.effect.Resource
import cats.effect.kernel.Async
import org.typelevel.log4cats.Logger

trait ContractsRepository[F[_]]:
  def save(contract: Contract): F[Unit]
  def updateIsMerged(subject: String, version: Int): F[Unit]
  def get(subject: String, version: Int): F[Option[Contract]]
  def getAll(): F[fs2.Stream[F, Contract]]
  def getAllVersionsForSubject(subject: String): F[fs2.Stream[F, Int]]
  def getAllSubjects(): F[fs2.Stream[F, String]]
  def getLatestContract(subject: String): F[Option[Contract]]
  def delete(subject: String, version: Int): F[Unit]
  def deleteSubject(subject: String): F[Unit]
  
object ContractsRepository:
  def make[F[_] : Async](session: Resource[F, Session[F]])(using Logger[F]): Resource[F, ContractsRepository[F]] =
    Resource.pure[F, ContractsRepository[F]](ContractsRepositoryPostgresImpl[F](session))