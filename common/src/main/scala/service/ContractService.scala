package io.github.sergeiionin.contractsregistrator
package service

import cats.data.EitherT
import dto.schema.{Versions, Subjects}
import dto.ContractDTO

trait ContractService[F[_]]:
  def getContractVersion(subject: String, version: Int): F[Option[ContractDTO]]
  def getContractVersions(subject: String): F[Versions]
  def getSubjects(): F[Subjects]
  def getLatestContract(subject: String): F[Option[ContractDTO]]
