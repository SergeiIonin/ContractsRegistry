package io.github.sergeiionin.contractsregistrator
package client.schemaregistry

import client.SchemasClient
import dto.errors.{HttpErrorDTO, BadRequestDTO, InternalServerErrorDTO}
import dto.schema.*
import http.client.ClientUtils

import cats.data.EitherT
import cats.effect.Async
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import cats.syntax.either.*
import io.confluent.kafka.schemaregistry.{ParsedSchema, SchemaProvider}
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema
import org.http4s.client.Client
import org.http4s.{EntityDecoder, EntityEncoder, Response, Uri}
import sttp.tapir.path

final class SchemaRegistryClientImpl[F[_] : Async](baseUri: String, client: Client[F]) extends SchemasClient[F]:
  import http4s.entitycodecs.CreateSchemaDtoEntityCodec.given
  import http4s.entitycodecs.CreateSchemaResponseDtoEntityCodec.given
  import http4s.entitycodecs.SchemaDtoEntityCodec.given
  import http4s.entitycodecs.SubjectsEntityCodec.given
  import http4s.entitycodecs.VersionEntityCodec.given
  import http4s.entitycodecs.VersionsEntityCodec.given

  import SchemaRegistryClientImpl.*
  
  private def toEitherT[R](rF: => F[R]): EitherT[F, HttpErrorDTO, R] =
    EitherT.liftF[F, HttpErrorDTO, R](rF)
  
  private def toLeftEitherT[R](err: HttpErrorDTO): EitherT[F, HttpErrorDTO, R] =
    EitherT.leftT[F, R](err)
  
  private def get(uri: Uri, token: Option[String]): EitherT[F, HttpErrorDTO, Response[F]] =
    toEitherT[Response[F]](
      client.run(ClientUtils.getRequest(uri, token))
        .use(resp => resp.pure[F])
    )

  private def post[T](uri: Uri, entity: T, token: Option[String])
                     (using EntityEncoder[F, T]): EitherT[F, HttpErrorDTO, Response[F]] =
    toEitherT[Response[F]](
      client.run(ClientUtils.postRequest(uri, token, entity))
        .use(resp => resp.pure[F])
    )

  private def delete(uri: Uri, token: Option[String]): EitherT[F, HttpErrorDTO, Response[F]] =
    toEitherT[Response[F]](
      client.run(ClientUtils.deleteRequest(uri, token))
        .use(resp => resp.pure[F])
    )
  
  private def responseOk(response: Response[F]): Boolean =
    response.status.code >= 200 && response.status.code < 300
  
  private def responseBad(response: Response[F]): Boolean =
    response.status.code >= 400 && response.status.code < 500
  
  private def convertResponse[R](response: Response[F])(errorMsg: => String)
                                (using EntityDecoder[F, R]): EitherT[F, HttpErrorDTO, R] =
    if (responseOk(response)) {
      toEitherT[R](response.as[R])
    } else if (responseBad(response)) {
      toLeftEitherT[R](BadRequestDTO(response.status.code, errorMsg))
    } else {
      toLeftEitherT[R](InternalServerErrorDTO())
    }
  
  override def createSchema(subject: String, schemaDTO: CreateSchemaDTO): EitherT[F, HttpErrorDTO, CreateSchemaResponseDTO] =
    for
      response <- post[CreateSchemaDTO](Uri.unsafeFromString(s"$baseUri/$subjects/$subject/$versions"),
                    schemaDTO, None)
      dto      <- convertResponse[CreateSchemaResponseDTO](response)("FIXME")
    yield dto    

  override def getSchemaVersion(subject: String, version: Int): EitherT[F, HttpErrorDTO, SchemaDTO] =
    for
      response <- get(Uri.unsafeFromString(s"$baseUri/$subjects/$subject/$versions/$version"), None)
      dto      <- convertResponse[SchemaDTO](response)("FIXME")
    yield dto  

  override def getSchemaVersions(subject: String): EitherT[F, HttpErrorDTO, Versions] =
    for 
      response <- get(Uri.unsafeFromString(s"$baseUri/$subjects/$subject/$versions"), None)
      versions <- convertResponse[Versions](response)("FIXME")
    yield versions

  override def getSubjects(): EitherT[F, HttpErrorDTO, Subjects] =
    for
      response  <- get(Uri.unsafeFromString(s"$baseUri/$subjects"), None)
      subjects <- convertResponse[Subjects](response)("FIXME")
    yield subjects
  
  override def getLatestVersion(subject: String): EitherT[F, HttpErrorDTO, SchemaDTO] =
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
                            case None => toLeftEitherT[SchemaDTO](BadRequestDTO(404, "No versions found"))
    yield schemaDto

  override def deleteSchemaVersion(subject: String, version: Int): EitherT[F, HttpErrorDTO, Version] =
    for
      response <- delete(Uri.unsafeFromString(s"$baseUri/$subjects/$subject/$versions/$version"), None)
      version  <- convertResponse[Version](response)("FIXME")
    yield version

  override def deleteSchemaSubject(subject: String): EitherT[F, HttpErrorDTO, Versions] =
    for
      response <- delete(Uri.unsafeFromString(s"$baseUri/$subjects/$subject"), None)
      versions <- convertResponse[Versions](response)("FIXME")
    yield versions

object SchemaRegistryClientImpl:
  val subjects = "subjects"
  val versions = "versions"
