package io.github.sergeiionin.contractsregistrator
package service

import domain.Contract
import dto.ContractDTO
import domain.{Subjects, Versions}
import repository.ContractsRepository

import cats.data.EitherT
import cats.effect.kernel.Resource

trait ContractService[F[_]]:
  def saveContract(contract: Contract): F[Unit]
  def getContractVersion(subject: String, version: Int): F[Option[Contract]]
  def getContractVersions(subject: String): F[fs2.Stream[F, Int]]
  def getSubjects(): F[fs2.Stream[F, String]]
  def getLatestContract(subject: String): F[Option[Contract]]
  def updateIsMerged(subject: String, version: Int): F[Unit]
  def deleteContractVersion(subject: String, version: Int): F[Unit]
  def deleteContract(subject: String): F[Unit]

object ContractService:
  def make[F[_]](contractRepo: ContractsRepository[F]): Resource[F, ContractService[F]] = 
    Resource.pure[F, ContractService[F]](ContractServiceImpl[F](contractRepo))