package io.github.sergeiionin.contractsregistrator
package service

import dto.SubjectAndVersionDTO
import dto.errors.{HttpErrorDTO, InternalServerErrorDTO}
import repository.ContractStatusRepository

import cats.data.EitherT
import cats.effect.{Async, Resource}
import cats.syntax.applicativeError.*
import cats.syntax.either.*
import cats.syntax.functor.*
import org.typelevel.log4cats.Logger

trait ContractStatusService[F[_]]:
  def updateIsMerged(subject: String, version: Int): EitherT[F, HttpErrorDTO, SubjectAndVersionDTO]
end ContractStatusService

object ContractStatusService:
  def make[F[_] : Async : Logger](repo: ContractStatusRepository[F]): Resource[F, ContractStatusService[F]] =
    Resource.pure[F, ContractStatusService[F]](new ContractStatusService[F] {
      override def updateIsMerged(subject: String, version: Int): EitherT[F, HttpErrorDTO, SubjectAndVersionDTO] =
        EitherT(repo.updateIsMerged(subject, version).attempt.map {
          case Right(_) => 
            SubjectAndVersionDTO(subject, version)
              .asRight[HttpErrorDTO]
          case Left(t) => 
            InternalServerErrorDTO(msg = s"Failed to update isMerged status: ${t.getMessage}")
              .asLeft[SubjectAndVersionDTO]
        })
    })