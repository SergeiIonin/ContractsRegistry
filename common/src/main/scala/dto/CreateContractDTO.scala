package io.github.sergeiionin.contractsregistrator
package dto

import domain.SchemaType
import domain.{Contract, SubjectAndVersion}
import domain.SchemaType.given
import dto.schema.SchemaDTO

import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

final case class ContractDTO(
    subject: String,
    version: Int,
    schemaType: SchemaType,
    schema: String)
    derives Encoder,
      Decoder,
      Schema
object ContractDTO:
  def fromSchemaDTO(schema: SchemaDTO) =
    ContractDTO(schema.subject, schema.version, schema.schemaType, schema.schema)
  def fromContract(contract: Contract): ContractDTO =
    ContractDTO(contract.subject, contract.version, contract.schemaType, contract.schema)

final case class SubjectAndVersionDTO(subject: String, version: Int)
    derives Encoder,
      Decoder,
      Schema
object SubjectAndVersionDTO:
  def fromSubjectAndVersion(snv: SubjectAndVersion): SubjectAndVersionDTO =
    SubjectAndVersionDTO(snv.subject, snv.version)

final case class CreateContractDTO(
    schemaType: String = "PROTOBUF",
    subject: String,
    schema: String)
    derives Encoder,
      Decoder,
      Schema
final case class CreateContractResponseDTO(name: String, id: Int)
    derives Encoder,
      Decoder,
      Schema

final case class DeleteContractVersionResponseDTO(subject: String, version: Int)
    derives Encoder,
      Decoder,
      Schema

final case class DeleteContractResponseDTO(subject: String) derives Encoder, Decoder, Schema
