package io.github.sergeiionin.contractsregistrator
package service

import repository.ContractsRepository
import domain.Contract

final class ContractServiceImpl[F[_]](contractsRepository: ContractsRepository[F]) extends ContractService[F]:
  def saveContract(contract: Contract): F[Unit] = contractsRepository.save(contract)
  def getContractVersion(subject: String, version: Int): F[Option[Contract]] = contractsRepository.get(subject, version)
  def getContractVersions(subject: String): F[fs2.Stream[F, Int]] = contractsRepository.getAllVersionsForSubject(subject)
  def getSubjects(): F[fs2.Stream[F, String]] = contractsRepository.getAllSubjects()
  def getLatestContract(subject: String): F[Option[Contract]] = contractsRepository.getLatestContract(subject)
  def deleteContractVersion(subject: String, version: Int): F[Unit] = contractsRepository.delete(subject, version)
  def deleteContract(subject: String): F[Unit] = contractsRepository.deleteSubject(subject)