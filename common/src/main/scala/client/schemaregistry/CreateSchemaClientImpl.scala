package io.github.sergeiionin.contractsregistrator
package client.schemaregistry

import client.CreateSchemaClient
import dto.errors.HttpErrorDTO
import dto.schema.*
import http.client.HttpClient
import cats.data.EitherT
import cats.effect.Async
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import org.http4s.Uri

final class CreateSchemaClientImpl[F[_] : Async](
                                                  baseUri: String,
                                                  client: HttpClient[F]
                                                ) extends CreateSchemaClient[F]
                                                  with ResponseMixin[F]
                                                  with SchemaRegistryPaths[F]:
  import http4s.entitycodecs.CreateSchemaDtoEntityCodec.given
  import http4s.entitycodecs.CreateSchemaResponseDtoEntityCodec.given

  override def createSchema(subject: String, schemaDTO: CreateSchemaDTO): EitherT[F, HttpErrorDTO, CreateSchemaResponseDTO] =
    val uri = Uri.unsafeFromString(s"$baseUri/$subjects/$subject/$versions")
    for
      response <- client.post[CreateSchemaDTO](uri, schemaDTO, None)
      dto      <- convertResponse[CreateSchemaResponseDTO](response)("FIXME") // fixme
    yield dto
