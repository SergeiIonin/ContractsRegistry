package io.github.sergeiionin.contractsregistrator
package client.schemaregistry

import client.SchemasClient
import dto.errors.{HttpErrorDTO, BadRequestDTO, InternalServerErrorDTO}
import dto.schema.*
import http.client.HttpClient

import cats.data.EitherT
import cats.effect.Async
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.either.*
import io.confluent.kafka.schemaregistry.{ParsedSchema, SchemaProvider}
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import org.http4s.client.Client
import org.http4s.{EntityDecoder, EntityEncoder, Response, Uri}
import sttp.tapir.path

final class SchemaRegistryClientImpl[F[_] : Async](baseUri: String, client: HttpClient[F]) extends SchemasClient[F]:
  import http4s.entitycodecs.CreateSchemaDtoEntityCodec.given
  import http4s.entitycodecs.CreateSchemaResponseDtoEntityCodec.given
  import http4s.entitycodecs.SchemaDtoEntityCodec.given
  import http4s.entitycodecs.SubjectsEntityCodec.given
  import http4s.entitycodecs.VersionEntityCodec.given
  import http4s.entitycodecs.VersionsEntityCodec.given

  import SchemaRegistryClientImpl.*
  import http.client.HttpClient.*
  
  private def responseOk(response: Response[F]): Boolean =
    response.status.code >= 200 && response.status.code < 300
  
  private def responseBad(response: Response[F]): Boolean =
    response.status.code >= 400 && response.status.code < 500
  
  private def convertResponse[R](response: Response[F])(errorMsg: => String)
                                (using EntityDecoder[F, R]): EitherT[F, HttpErrorDTO, R] =
    if (responseOk(response)) {
      response.as[R].toEitherT
    } else if (responseBad(response)) {
      BadRequestDTO(response.status.code, errorMsg).toLeftEitherT[R]
    } else {
      InternalServerErrorDTO().toLeftEitherT[R]
    }
  
  override def createSchema(subject: String, schemaDTO: CreateSchemaDTO): EitherT[F, HttpErrorDTO, CreateSchemaResponseDTO] =
    for
      response <- client.post[CreateSchemaDTO](Uri.unsafeFromString(s"$baseUri/$subjects/$subject/$versions"),
                    schemaDTO, None)
      dto      <- convertResponse[CreateSchemaResponseDTO](response)("FIXME")
    yield dto    

  override def getSchemaVersion(subject: String, version: Int): EitherT[F, HttpErrorDTO, SchemaDTO] =
    for
      response <- client.get(Uri.unsafeFromString(s"$baseUri/$subjects/$subject/$versions/$version"), None)
      dto      <- convertResponse[SchemaDTO](response)("FIXME")
    yield dto  

  override def getSchemaVersions(subject: String): EitherT[F, HttpErrorDTO, Versions] =
    for 
      response <- client.get(Uri.unsafeFromString(s"$baseUri/$subjects/$subject/$versions"), None)
      versions <- convertResponse[Versions](response)("FIXME")
    yield versions

  override def getSubjects(): EitherT[F, HttpErrorDTO, Subjects] =
    for
      response  <- client.get(Uri.unsafeFromString(s"$baseUri/$subjects"), None)
      subjects <- convertResponse[Subjects](response)("FIXME")
    yield subjects
  
  override def getLatestSchema(subject: String): EitherT[F, HttpErrorDTO, SchemaDTO] =
    def getLatestVersion(subject: String) =
      for
        versions <- getSchemaVersions(subject)
        latest = Versions.toList(versions).sorted.lastOption
      yield latest

    def getSchema(subject: String, version: Int) =
      getSchemaVersion(subject, version)
    
    for
        latestVersion <- getLatestVersion(subject)
        schemaDto     <- latestVersion match
                            case Some(version) => getSchema(subject, version)
                            case None => BadRequestDTO(404, "No versions found").toLeftEitherT[SchemaDTO]
    yield schemaDto

  override def deleteSchemaVersion(subject: String, version: Int): EitherT[F, HttpErrorDTO, Version] =
    for
      response <- client.delete(Uri.unsafeFromString(s"$baseUri/$subjects/$subject/$versions/$version"), None)
      version  <- convertResponse[Version](response)("FIXME")
    yield version

  override def deleteSchemaSubject(subject: String): EitherT[F, HttpErrorDTO, Versions] =
    for
      response <- client.delete(Uri.unsafeFromString(s"$baseUri/$subjects/$subject"), None)
      versions <- convertResponse[Versions](response)("FIXME")
    yield versions

object SchemaRegistryClientImpl:
  val subjects = "subjects"
  val versions = "versions"
