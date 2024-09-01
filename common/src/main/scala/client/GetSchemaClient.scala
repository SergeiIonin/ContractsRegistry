package io.github.sergeiionin.contractsregistrator
package client

import http.client.HttpClient
import client.schemaregistry.GetSchemaClientImpl
import dto.errors.HttpErrorDTO
import dto.schema.*
import domain.{Version, Versions, Subjects}
import cats.data.EitherT
import cats.effect.{Async, Resource}

trait GetSchemaClient[F[_]] extends SchemaClient[F]:
  def getSchemaVersion(subject: String, version: Int): EitherT[F, HttpErrorDTO, SchemaDTO]
  def getSchemaVersions(subject: String): EitherT[F, HttpErrorDTO, Versions]
  def getSubjects(): EitherT[F, HttpErrorDTO, Subjects]
  def getLatestSchema(subject: String): EitherT[F, HttpErrorDTO, SchemaDTO]

object GetSchemaClient:
  def make[F[_]: Async](
      baseUri: String,
      httpClient: HttpClient[F]): Resource[F, GetSchemaClient[F]] =
    Resource.pure[F, GetSchemaClient[F]](GetSchemaClientImpl(baseUri, httpClient))
