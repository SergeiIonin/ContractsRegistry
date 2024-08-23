package io.github.sergeiionin.contractsregistrator
package http.client

import domain.SchemaType
import dto.schema.{CreateSchemaDTO, SchemaDTO}

final class TestSchemaStorage():
  private val idToSchema = collection.mutable.Map.empty[String, SchemaDTO]
  private val subjectToLatestVersion = collection.mutable.Map.empty[String, Int]
  
  private def getSchemaId(subject: String, version: Int): String =
    s"${subject}_$version"
  
  private def getSchemaId(schema: SchemaDTO): String =
    getSchemaId(schema.subject, schema.version)
  
  def add(subject: String, schemaDTO: CreateSchemaDTO): Int =
    val versionUpd = subjectToLatestVersion.get(subject).map(_ + 1).getOrElse(1)
    subjectToLatestVersion.update(subject, versionUpd)
    val schema = SchemaDTO(subject, versionUpd, versionUpd, SchemaType.PROTOBUF, schemaDTO.schema)
    idToSchema += (getSchemaId(schema) -> schema)
    versionUpd

  def get(subject: String, version: Int): Either[String, SchemaDTO] =
    val id = getSchemaId(subject, version)
    if !idToSchema.contains(id) then
      Left(s"Schema with subject $subject and version $version not found")
    else
      Right(idToSchema(id))
  
  def getVersions(subject: String): Either[String, List[Int]] =
    val versions = idToSchema.collect {
      case (id, schema) if schema.subject == subject => schema.version
    }.toList
    if versions.isEmpty then
      Left(s"Schemas with subject $subject not found")
    else
      Right(versions)
  
  def getSubjects: List[String] =
    idToSchema.values.map(_.subject).toList.distinct
  
  def delete(subject: String, version: Int): Either[String, Int] =
    val id = getSchemaId(subject, version)
    
    if !idToSchema.contains(id) then
      Left(s"Schema with subject $subject and version $version not found")
    else
      val latestVersion = subjectToLatestVersion(subject)
      if (latestVersion == version) {
        if (version == 1) {
          subjectToLatestVersion -= subject
        } else {
          subjectToLatestVersion.update(subject, version - 1)
        }
      }
      idToSchema -= id
      Right(version)
  
  def deleteSubject(subject: String): Either[String, List[Int]] =
    subjectToLatestVersion.remove(subject)
    
    val ids = idToSchema.collect {
      case (id, schema) if schema.subject == subject => id
    }.toList
    if ids.isEmpty then
      Left(s"Schemas with subject $subject not found")
    else
      ids.foreach(id => idToSchema -= id)
      val versions = ids.map(id => id.split("_").last.toInt)
      Right(versions)