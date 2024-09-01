package io.github.sergeiionin.contractsregistrator
package service

import domain.SchemaType
import dto.{ContractDTO, CreateContractDTO}

final class TestContractStorage():
  private val idToContract = collection.mutable.Map.empty[String, ContractDTO]
  private val subjectToLatestVersion = collection.mutable.Map.empty[String, Int]

  private def getContractId(subject: String, version: Int): String =
    s"${subject}_$version"

  private def getContractId(contract: ContractDTO): String =
    getContractId(contract.subject, contract.version)

  def add(subject: String, contractDTO: CreateContractDTO): Int =
    val versionUpd = subjectToLatestVersion.get(subject).map(_ + 1).getOrElse(1)
    subjectToLatestVersion.update(subject, versionUpd)
    val contract = ContractDTO(subject, versionUpd, SchemaType.PROTOBUF, contractDTO.schema)
    idToContract += (getContractId(contract) -> contract)
    versionUpd

  def get(subject: String, version: Int): Either[String, ContractDTO] =
    val id = getContractId(subject, version)
    if !idToContract.contains(id) then
      Left(s"Contract with subject $subject and version $version not found")
    else Right(idToContract(id))

  def getVersions(subject: String): Either[String, List[Int]] =
    val versions = idToContract.collect {
      case (id, contract) if contract.subject == subject => contract.version
    }.toList
    if versions.isEmpty then Left(s"Contracts with subject $subject not found")
    else Right(versions)

  def getSubjects(): List[String] =
    idToContract.values.map(_.subject).toList.distinct

  def delete(subject: String, version: Int): Either[String, Int] =
    val id = getContractId(subject, version)

    if !idToContract.contains(id) then
      Left(s"Contract with subject $subject and version $version not found")
    else
      val latestVersion = subjectToLatestVersion(subject)
      if (latestVersion == version) {
        if (version == 1) {
          subjectToLatestVersion -= subject
        } else {
          subjectToLatestVersion.update(subject, version - 1)
        }
      }
      idToContract -= id
      Right(version)

  def deleteSubject(subject: String): Either[String, List[Int]] =
    subjectToLatestVersion.remove(subject)

    val ids = idToContract.collect {
      case (id, contract) if contract.subject == subject => id
    }.toList
    if ids.isEmpty then Left(s"Contracts with subject $subject not found")
    else
      ids.foreach(id => idToContract -= id)
      val versions = ids.map(id => id.split("_").last.toInt)
      Right(versions)
