package io.github.sergeiionin.contractsregistrator
package service

import domain.Contract
import dto.ContractDTO
import dto.errors.{BadRequestDTO, HttpErrorDTO, InternalServerErrorDTO}
import repository.ContractsRepository

import cats.data.EitherT
import cats.effect.Concurrent
import cats.syntax.applicativeError.*
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.{Monad, MonadThrow}
import fs2.Stream.*

final class ContractServiceImpl[F[_]: MonadThrow: Concurrent](
    contractsRepository: ContractsRepository[F])
    extends ContractService[F]:
  private def asLeft[R](err: => HttpErrorDTO): Either[HttpErrorDTO, R] =
    err.asLeft[R]

  override def saveContract(contract: Contract): EitherT[F, HttpErrorDTO, Unit] =
    EitherT(
      contractsRepository.save(contract).attempt.map {
        case Right(_) => ().asRight[HttpErrorDTO]
        case Left(t) =>
          asLeft[Unit](
            InternalServerErrorDTO(msg = s"Failed to save contract: ${t.getMessage}"))
      }
    )
  override def getContractVersion(
      subject: String,
      version: Int): EitherT[F, HttpErrorDTO, ContractDTO] =
    EitherT(contractsRepository.get(subject, version).attempt.map {
      case Right(contract) if contract.isDefined =>
        ContractDTO.fromContract(contract.get).asRight[HttpErrorDTO]
      case Right(_) =>
        asLeft[ContractDTO](
          BadRequestDTO(404, s"Contract with subject $subject and version $version not found"))
      case Left(t) =>
        asLeft[ContractDTO](
          InternalServerErrorDTO(msg = s"Failed to get contract version: ${t.getMessage}"))
    })
  override def getContractVersions(subject: String): EitherT[F, HttpErrorDTO, List[Int]] =
    EitherT(
      contractsRepository
        .getAllVersionsForSubject(subject)
        .flatMap(_.compile.toList)
        .attempt
        .map {
          case Right(versions) => versions.asRight[HttpErrorDTO]
          case Left(t) =>
            asLeft[List[Int]](
              InternalServerErrorDTO(msg = s"Failed to get contract versions: ${t.getMessage}"))
        }
    )
  override def getSubjects(): EitherT[F, HttpErrorDTO, List[String]] =
    EitherT(
      contractsRepository.getAllSubjects().flatMap(_.compile.toList).attempt.map {
        case Right(subjects) => subjects.asRight[HttpErrorDTO]
        case Left(t) =>
          asLeft[List[String]](
            InternalServerErrorDTO(msg = s"Failed to get subjects: ${t.getMessage}"))
      }
    )
  override def getLatestContract(subject: String): EitherT[F, HttpErrorDTO, ContractDTO] =
    EitherT(
      contractsRepository
        .getLatestContract(subject)
        .attempt
        .map {
          case Right(contract) if contract.isDefined =>
            ContractDTO.fromContract(contract.get).asRight[HttpErrorDTO]
          case Right(_) =>
            asLeft[ContractDTO](BadRequestDTO(404, s"Contract with subject $subject not found"))
          case Left(t) =>
            asLeft[ContractDTO](
              InternalServerErrorDTO(msg = s"Failed to get latest contract: ${t.getMessage}"))
        }
    )
  // todo return 404 if contract not found?
  override def deleteContractVersion(
      subject: String,
      version: Int): EitherT[F, HttpErrorDTO, Unit] =
    EitherT(
      contractsRepository.delete(subject, version).attempt.map {
        case Right(_) => ().asRight[HttpErrorDTO]
        case Left(t) =>
          asLeft[Unit](
            InternalServerErrorDTO(msg = s"Failed to delete contract version: ${t.getMessage}"))
      }
    )
  // todo return 404 if contract not found?
  override def deleteContract(subject: String): EitherT[F, HttpErrorDTO, Unit] =
    EitherT(
      contractsRepository.deleteSubject(subject).attempt.map {
        case Right(_) => ().asRight[HttpErrorDTO]
        case Left(t) =>
          asLeft[Unit](
            InternalServerErrorDTO(msg = s"Failed to delete contract: ${t.getMessage}"))
      }
    )
