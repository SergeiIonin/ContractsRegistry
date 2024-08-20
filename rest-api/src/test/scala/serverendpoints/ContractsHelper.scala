package io.github.sergeiionin.contractsregistrator
package serverendpoints

import dto.{ContractDTO, ContractErrorDTO, CreateContractDTO,
  CreateContractResponseDTO, DeleteContractResponseDTO, DeleteContractVersionResponseDTO}
import io.circe.Encoder
import org.http4s.circe.jsonEncoderOf
import org.http4s.EntityEncoder

trait ContractsHelper:
  import ContractHelper.*
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
  def createContractDTOJson(subject: String, schema: String) = CreateContractDTOEncoder.apply(createContractDTO(subject, schema)).toString
  
  val contractDTOv1: ContractDTO = ContractDTO(schema = schemaV1)
  val contractDTOv2: ContractDTO = ContractDTO(schema = schemaV2)

object ContractHelper:
  val CreateContractDTOEncoder = summon[Encoder[CreateContractDTO]]
  given createContractDtoEncoder[F[_]]: EntityEncoder[F, CreateContractDTO] = jsonEncoderOf[F, CreateContractDTO]

  