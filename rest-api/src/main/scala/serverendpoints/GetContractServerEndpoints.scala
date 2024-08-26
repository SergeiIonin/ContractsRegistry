package io.github.sergeiionin.contractsregistrator
package serverendpoints

import dto.*
import dto.errors.{BadRequestDTO, HttpErrorDTO}
import dto.schema.*
import domain.{Versions, Subjects}
import endpoints.GetContractEndpoints
import service.ContractService
import cats.effect.Async
import cats.syntax.either.*
import cats.syntax.functor.*
import cats.syntax.flatMap.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.server.ServerEndpoint.Full

class GetContractServerEndpoints[F[_] : Async](service: ContractService[F]) extends GetContractEndpoints:
  private val getContractVersionSE: ServerEndpoint[Any, F] =
    getContractVersion.serverLogic((subject, version) => {
      service
        .getContractVersion(subject, version)
        .map {
          case Some(contract) => ContractDTO.fromContract(contract).asRight[HttpErrorDTO]
          case None => BadRequestDTO(404, s"Contract with subject $subject and version $version not found").asLeft[ContractDTO]
        }
    })
  
  private val getVersionsSE: ServerEndpoint[Any, F] =
    getVersions.serverLogic(subject => {
      for
        stream    <- service.getContractVersions(subject)
        versions  <- stream.compile.toList
        res       = versions match {
                      case Nil      => BadRequestDTO(404, s"No versions found for subject $subject").asLeft[List[Int]]
                      case versions => versions.asRight[HttpErrorDTO]
                    }
      yield res
    })
  
  private val getSubjectsSE: ServerEndpoint[Any, F] =
    getSubjects.serverLogic(_ => {
      for
        stream    <- service.getSubjects()
        subjects  <- stream.compile.toList
        res       = subjects match {
                      case Nil => BadRequestDTO(404, "No subjects found").asLeft[List[String]]
                      case subjects => subjects.asRight[HttpErrorDTO]
                    }
      yield res
    })
  
  private val getLatestContractSE: ServerEndpoint[Any, F] =
    getLatestContract.serverLogic(subject => {
      service
        .getLatestContract(subject)
        .map {
          case Some(contract) => ContractDTO.fromContract(contract).asRight[HttpErrorDTO]
          case None => BadRequestDTO(404, s"Contract with subject $subject not found").asLeft[ContractDTO]
        }
    })
  
  private val getServerEndpoints: List[ServerEndpoint[Any, F]] =
    List(getContractVersionSE, getVersionsSE, getSubjectsSE, getLatestContractSE)

  val serverEndpoints = getServerEndpoints
