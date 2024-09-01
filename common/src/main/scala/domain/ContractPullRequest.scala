package io.github.sergeiionin.contractsregistrator
package domain

import scala.util.{Try, Success, Failure}

final case class ContractPullRequest(subject: String, version: Int, isDeleted: Boolean):
  private val subjectAndVersion = s"${subject}_$version"
  def getBody(): String = subjectAndVersion
  def getTitle(): String = if !isDeleted then s"Add contract $subjectAndVersion"
  else s"Delete contract $subjectAndVersion"

object ContractPullRequest:
  def fromContract(contract: Contract): ContractPullRequest =
    ContractPullRequest(contract.subject, contract.version, contract.deleted.getOrElse(false))
  def fromRaw(raw: String, isDeleted: Boolean): Either[String, ContractPullRequest] =
    raw.split("_").toList match
      case subject :: version :: Nil =>
        Try(version.toInt) match
          case Success(version) => Right(ContractPullRequest(subject, version, isDeleted))
          case Failure(_) => Left(s"version $version is not a valid integer")
      case _ =>
        Left(
          s"Invalid contract pull request format: $raw, the format should be <subject>_<version>")
