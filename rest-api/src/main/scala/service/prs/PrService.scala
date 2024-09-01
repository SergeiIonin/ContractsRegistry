package io.github.sergeiionin.contractsregistrator
package service.prs

import client.{GetSchemaClient, DeleteSchemaClient}
import domain.ContractPullRequest
import dto.SubjectAndVersionDTO
import dto.errors.HttpErrorDTO
import service.ContractService

import cats.Monad
import cats.data.EitherT
import cats.effect.{Async, Resource}

trait PrService[F[_]]:
  def processPR(pr: ContractPullRequest): EitherT[F, HttpErrorDTO, SubjectAndVersionDTO]

object PrService:
  def make[F[_]: Async](
      contractService: ContractService[F],
      getClient: GetSchemaClient[F],
      deleteClient: DeleteSchemaClient[F]
  ): Resource[F, PrService[F]] =
    Resource.pure[F, PrService[F]](PrServiceImpl[F](contractService, getClient, deleteClient))
