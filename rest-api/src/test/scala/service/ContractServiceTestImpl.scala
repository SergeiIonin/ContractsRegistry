package io.github.sergeiionin.contractsregistrator
package service

import cats.data.EitherT
import cats.Monad
import cats.syntax.either.*
import cats.syntax.applicative.*
import cats.syntax.functor.*
import domain.Contract
import dto.{ContractDTO, CreateContractDTO}
import dto.errors.{BadRequestDTO, HttpErrorDTO}

class ContractServiceTestImpl[F[_]: Monad] extends ContractService[F]:
  private val storage = TestContractStorage()

  private def asLeft[R](err: => HttpErrorDTO): Either[HttpErrorDTO, R] =
    err.asLeft[R]

  override def saveContract(contract: Contract): EitherT[F, HttpErrorDTO, Unit] =
    EitherT(
      storage
        .add(
          contract.subject,
          CreateContractDTO(subject = contract.subject, schema = contract.schema))
        .asRight[HttpErrorDTO]
        .pure[F]
    ).map(_ => ())

  override def getContractVersion(
      subject: String,
      version: Int): EitherT[F, HttpErrorDTO, ContractDTO] =
    storage.get(subject, version) match
      case Right(contract) => EitherT(contract.asRight[HttpErrorDTO].pure[F])
      case Left(err) => EitherT(asLeft[ContractDTO](BadRequestDTO(404, err)).pure[F])

  override def getContractVersions(subject: String): EitherT[F, HttpErrorDTO, List[Int]] =
    storage.getVersions(subject) match
      case Right(versions) => EitherT(versions.asRight[HttpErrorDTO].pure[F])
      case Left(err) => EitherT(asLeft[List[Int]](BadRequestDTO(404, err)).pure[F])

  override def getSubjects(): EitherT[F, HttpErrorDTO, List[String]] =
    val subjects = storage.getSubjects()
    if subjects.isEmpty then
      EitherT(asLeft[List[String]](BadRequestDTO(404, "No subjects found")).pure[F])
    else EitherT(subjects.asRight[HttpErrorDTO].pure[F])

  override def getLatestContract(subject: String): EitherT[F, HttpErrorDTO, ContractDTO] =
    storage.getVersions(subject) match
      case Right(versions) =>
        if versions.isEmpty then
          EitherT(
            asLeft[ContractDTO](BadRequestDTO(404, s"No versions found for subject $subject"))
              .pure[F])
        else getContractVersion(subject, versions.max)
      case Left(err) => EitherT(asLeft[ContractDTO](BadRequestDTO(404, err)).pure[F])

  override def deleteContractVersion(
      subject: String,
      version: Int): EitherT[F, HttpErrorDTO, Unit] =
    storage.delete(subject, version) match
      case Right(_) => EitherT(().asRight[HttpErrorDTO].pure[F])
      case Left(err) => EitherT(asLeft[Unit](BadRequestDTO(404, err)).pure[F])

  override def deleteContract(subject: String): EitherT[F, HttpErrorDTO, Unit] =
    storage.deleteSubject(subject) match
      case Right(_) => EitherT(().asRight[HttpErrorDTO].pure[F])
      case Left(err) => EitherT(asLeft[Unit](BadRequestDTO(404, err)).pure[F])
