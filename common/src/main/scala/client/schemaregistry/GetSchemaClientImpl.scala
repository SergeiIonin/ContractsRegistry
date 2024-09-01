package io.github.sergeiionin.contractsregistrator
package client.schemaregistry

import client.GetSchemaClient
import client.schemaregistry.{ResponseMixin, SchemaRegistryPaths}
import domain.{Version, Versions, Subjects}
import dto.errors.{HttpErrorDTO, BadRequestDTO}
import dto.schema.*
import http.client.HttpClient

import cats.data.EitherT
import cats.effect.Async
import cats.syntax.applicative.*
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.option.*
import org.http4s.{EntityDecoder, EntityEncoder, Response, Uri}

final class GetSchemaClientImpl[F[_]: Async](baseUri: String, client: HttpClient[F])
    extends GetSchemaClient[F]
    with ResponseMixin[F]
    with SchemaRegistryPaths[F]:
  import http4s.entitycodecs.VersionEntityCodec.given
  import http4s.entitycodecs.VersionsEntityCodec.given
  import http4s.entitycodecs.SubjectsEntityCodec.given
  import http4s.entitycodecs.SchemaDtoEntityCodec.given
  import http.client.extensions.*

  override def getSchemaVersion(
      subject: String,
      version: Int): EitherT[F, HttpErrorDTO, SchemaDTO] =
    for
      response <- client.get(
        Uri.unsafeFromString(s"$baseUri/$subjects/$subject/$versions/$version"),
        None)
      dto <- convertResponse[SchemaDTO](response)("FIXME") // fixme
    yield dto

  override def getSchemaVersions(subject: String): EitherT[F, HttpErrorDTO, Versions] =
    for
      response <- client.get(
        Uri.unsafeFromString(s"$baseUri/$subjects/$subject/$versions"),
        None)
      versions <- convertResponse[Versions](response)("FIXME") // fixme
    yield versions

  override def getSubjects(): EitherT[F, HttpErrorDTO, Subjects] =
    for
      response <- client.get(Uri.unsafeFromString(s"$baseUri/$subjects"), None)
      subjects <- convertResponse[Subjects](response)("FIXME") // fixme
    yield subjects

  override def getLatestSchema(subject: String): EitherT[F, HttpErrorDTO, SchemaDTO] =
    def getLatestVersion(subject: String): EitherT[F, HttpErrorDTO, Option[Int]] =
      for
        versions <- getSchemaVersions(subject)
        latest = Versions.toList(versions).sorted.lastOption
      yield latest

    def getSchema(subject: String, version: Int): EitherT[F, HttpErrorDTO, SchemaDTO] =
      getSchemaVersion(subject, version)

    for
      latestVersion <- getLatestVersion(subject)
      schemaDto <- latestVersion match
        case Some(version) => getSchema(subject, version)
        case None => BadRequestDTO(404, "No versions found").toLeftEitherT[SchemaDTO]
    yield schemaDto
