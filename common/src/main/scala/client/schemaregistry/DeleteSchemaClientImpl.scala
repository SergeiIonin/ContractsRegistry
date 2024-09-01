package io.github.sergeiionin.contractsregistrator
package client.schemaregistry

import client.schemaregistry.{ResponseMixin, SchemaRegistryPaths}
import domain.{Version, Versions}
import client.DeleteSchemaClient
import dto.errors.HttpErrorDTO
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

final class DeleteSchemaClientImpl[F[_] : Async](baseUri: String,
                                                 client: HttpClient[F]) extends DeleteSchemaClient[F]
                                                                        with ResponseMixin[F]
                                                                        with SchemaRegistryPaths[F]:
  import http4s.entitycodecs.VersionEntityCodec.given
  import http4s.entitycodecs.VersionsEntityCodec.given

  override def deleteSchemaVersion(subject: String, version: Int): EitherT[F, HttpErrorDTO, Version] =
    val uri = Uri.unsafeFromString(s"$baseUri/$subjects/$subject/$versions/$version")
    for
      response <- client.delete(uri, None)
      version  <- convertResponse[Version](response)("FIXME") // fixme
    yield version

  override def deleteSchemaSubject(subject: String): EitherT[F, HttpErrorDTO, Versions] =
    val uri = Uri.unsafeFromString(s"$baseUri/$subjects/$subject")
    for
      response <- client.delete(uri, None)
      versions <- convertResponse[Versions](response)("FIXME") // fixme
    yield versions
