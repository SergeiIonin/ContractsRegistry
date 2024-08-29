package io.github.sergeiionin.contractsregistrator
package service

import repository.ContractsRepository
import domain.Contract

final class ContractServiceImpl[F[_]](contractsRepository: ContractsRepository[F]) extends ContractService[F]:
  override def saveContract(contract: Contract): F[Unit] = contractsRepository.save(contract)
  override def getContractVersion(subject: String, version: Int): F[Option[Contract]] = contractsRepository.get(subject, version)
  override def getContractVersions(subject: String): F[fs2.Stream[F, Int]] = contractsRepository.getAllVersionsForSubject(subject)
  override def getSubjects(): F[fs2.Stream[F, String]] = contractsRepository.getAllSubjects()
  override def getLatestContract(subject: String): F[Option[Contract]] = contractsRepository.getLatestContract(subject)
  override def updateIsMerged(subject: String, version: Int): F[Unit] = contractsRepository.updateIsMerged(subject, version)
  override def deleteContractVersion(subject: String, version: Int): F[Unit] = contractsRepository.delete(subject, version)
  override def deleteContract(subject: String): F[Unit] = contractsRepository.deleteSubject(subject)