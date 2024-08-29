package io.github.sergeiionin.contractsregistrator
package service.prs

import client.DeleteSchemaClient
import domain.ContractPullRequest
import dto.SubjectAndVersionDTO
import dto.errors.{HttpErrorDTO, InternalServerErrorDTO}
import service.{ContractService, ContractStatusService}

import cats.data.EitherT
import cats.syntax.applicativeError.*
import cats.syntax.either.*
import cats.syntax.functor.*
import cats.{Monad, MonadThrow}

class PrServiceImpl[F[_] : Monad : MonadThrow](
                                                contractStatusService: ContractStatusService[F],
                                                deleteClient: DeleteSchemaClient[F]
                                              ) extends PrService[F]:
  override def processPR(pr: ContractPullRequest): EitherT[F, HttpErrorDTO, SubjectAndVersionDTO] =
    if pr.isDeleted then
      deleteContractVersion(pr.subject, pr.version)
    else
      updateIsMerged(pr.subject, pr.version)
  
  private def updateIsMerged(subject: String, version: Int): EitherT[F, HttpErrorDTO, SubjectAndVersionDTO] =
    contractStatusService.updateIsMerged(subject, version)
  
  private def deleteContractVersion(subject: String, version: Int): EitherT[F, HttpErrorDTO, SubjectAndVersionDTO] =
    deleteClient.deleteSchemaVersion(subject, version)
      .map(_ => SubjectAndVersionDTO(subject, version))
