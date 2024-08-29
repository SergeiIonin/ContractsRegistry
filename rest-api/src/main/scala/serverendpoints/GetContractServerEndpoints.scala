package io.github.sergeiionin.contractsregistrator
package serverendpoints

import dto.*
import dto.errors.{BadRequestDTO, HttpErrorDTO}
import dto.schema.*
import domain.{Versions, Subjects}
import endpoints.GetContractEndpoints
import service.ContractService
import cats.data.EitherT
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
        .value
    })
  
  private val getVersionsSE: ServerEndpoint[Any, F] =
    getVersions.serverLogic(subject => {
      service
        .getContractVersions(subject)
        .value
    })
  
  private val getSubjectsSE: ServerEndpoint[Any, F] =
    getSubjects.serverLogic(_ => {
      service
        .getSubjects()
        .value
    })
  
  private val getLatestContractSE: ServerEndpoint[Any, F] =
    getLatestContract.serverLogic(subject => {
      service
        .getLatestContract(subject)
        .value
    })
  
  private val getServerEndpoints: List[ServerEndpoint[Any, F]] =
    List(getContractVersionSE, getVersionsSE, getSubjectsSE, getLatestContractSE)

  val serverEndpoints = getServerEndpoints
