package io.github.sergeiionin.contractsregistrator
package dto.schema

import io.circe.{Decoder, Encoder}
import domain.SchemaType
import sttp.tapir.Schema

opaque type Subjects = List[String]
object Subjects:
  def apply(subjects: List[String]): Subjects = subjects
  def toList(subjects: Subjects): List[String] = subjects

opaque type Version = Int
object Version:
  def apply(version: Int): Version = version
  def toInt(version: Version): Int = version

opaque type Versions = List[Int]
object Versions:
  def apply(versions: List[Int]): Versions = versions
  def toList(versions: Versions): List[Int] = versions

extension (versions: Versions)
  def headOption: Option[Int] = versions.headOption

final case class CreateSchemaResponseDTO(id: Int) derives Encoder, Decoder, Schema
final case class CreateSchemaDTO(schemaType: String = "PROTOBUF", schema: String)derives Encoder, Decoder, Schema

final case class SchemaDTO(subject: String, version: Int, id: Int, schemaType: SchemaType, schema: String) derives Encoder, Decoder, Schema
