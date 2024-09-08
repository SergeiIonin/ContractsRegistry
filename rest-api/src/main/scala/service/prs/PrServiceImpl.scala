package io.github.sergeiionin.contractsregistrator
package service.prs

import cats.syntax.option.*
import client.{GetSchemaClient, DeleteSchemaClient}
import domain.{Contract, ContractPullRequest}
import dto.SubjectAndVersionDTO
import dto.errors.{HttpErrorDTO, InternalServerErrorDTO}
import service.ContractService

import cats.data.EitherT
import cats.syntax.applicativeError.*
import cats.syntax.either.*
import cats.syntax.functor.*
import cats.{Monad, MonadThrow}

class PrServiceImpl[F[_]: Monad: MonadThrow](
    contractsService: ContractService[F],
    getClient: GetSchemaClient[F],
    deleteClient: DeleteSchemaClient[F]
) extends PrService[F]:
  override def processPR(
      pr: ContractPullRequest): EitherT[F, HttpErrorDTO, SubjectAndVersionDTO] =
    if pr.isDeleted then deleteContractVersion(pr.subject, pr.version)
    else addContract(pr.subject, pr.version)

  private def addContract(
      subject: String,
      version: Int): EitherT[F, HttpErrorDTO, SubjectAndVersionDTO] =
    for
      schema <- getClient.getSchemaVersion(subject, version)
      contract = Contract(
        subject,
        version,
        schema.id,
        schema.schema,
        schema.schemaType
        )
      _ <- contractsService.saveContract(contract)
    yield SubjectAndVersionDTO(subject, version)

  // todo we shouldn't fail of the contract was not found
  private def deleteContractVersion(
      subject: String,
      version: Int): EitherT[F, HttpErrorDTO, SubjectAndVersionDTO] =
    for
      _ <- contractsService.deleteContractVersion(subject, version)
      _ <- deleteClient.deleteSchemaVersion(subject, version)
    yield SubjectAndVersionDTO(subject, version)
