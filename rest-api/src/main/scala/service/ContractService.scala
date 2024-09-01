package io.github.sergeiionin.contractsregistrator
package service

import domain.{Contract, Subjects, Versions}
import dto.ContractDTO
import dto.errors.HttpErrorDTO
import repository.ContractsRepository

import cats.MonadThrow
import cats.data.EitherT
import cats.effect.Concurrent
import cats.effect.kernel.Resource

trait ContractService[F[_]]:
  def saveContract(contract: Contract): EitherT[F, HttpErrorDTO, Unit]
  def getContractVersion(subject: String, version: Int): EitherT[F, HttpErrorDTO, ContractDTO]
  def getContractVersions(subject: String): EitherT[F, HttpErrorDTO, List[Int]]
  def getSubjects(): EitherT[F, HttpErrorDTO, List[String]]
  def getLatestContract(subject: String): EitherT[F, HttpErrorDTO, ContractDTO]
  def deleteContractVersion(subject: String, version: Int): EitherT[F, HttpErrorDTO, Unit]
  def deleteContract(subject: String): EitherT[F, HttpErrorDTO, Unit]

object ContractService:
  def make[F[_]: MonadThrow: Concurrent](
      contractRepo: ContractsRepository[F]): Resource[F, ContractService[F]] =
    Resource.pure[F, ContractService[F]](ContractServiceImpl[F](contractRepo))
