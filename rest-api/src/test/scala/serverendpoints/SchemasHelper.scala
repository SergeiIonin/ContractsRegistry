package io.github.sergeiionin.contractsregistrator
package serverendpoints

import dto.{CreateContractDTO,
  CreateContractResponseDTO, DeleteContractResponseDTO, DeleteContractVersionResponseDTO}
import dto.schema.CreateSchemaDTO
import dto.errors.HttpErrorDTO
import io.circe.Encoder
import org.http4s.circe.jsonEncoderOf
import org.http4s.EntityEncoder

trait SchemasHelper:
  import SchemasHelper.*
  val schemaV1 =
    """
      | syntax = \"proto3\";\n
      | package Foo;\n\
      | message Bar {\n
      |  string a = 1;\n
      |  int32 b = 2;\n
      |  int32 c = 3;\n
      |  string f = 4;\n
      | }\n
      |""".stripMargin
  val schemaV2 =
    """
      | syntax = \"proto3\";\n
      | package Foo;\n\
      | message Bar {\n
      |  string a = 1;\n
      |  int32 b = 2;\n
      |  int32 c = 3;\n
      | }\n
      |""".stripMargin
  private def createContractDTO(subject: String, schema: String) = CreateContractDTO(subject = subject, schema = schema)
  def createContractDTOJson(subject: String, schema: String) = createContractDTOEncoder.apply(createContractDTO(subject, schema)).toString
  
  val schemaDTOv1: CreateSchemaDTO = CreateSchemaDTO(schema = schemaV1)
  val schemaDTOv2: CreateSchemaDTO = CreateSchemaDTO(schema = schemaV2)

object SchemasHelper:
  val createContractDTOEncoder = summon[Encoder[CreateContractDTO]]
  given createContractDtoEntityEncoder[F[_]]: EntityEncoder[F, CreateContractDTO] = jsonEncoderOf[F, CreateContractDTO]

  