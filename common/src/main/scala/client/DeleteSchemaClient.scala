package io.github.sergeiionin.contractsregistrator
package client

import http.client.HttpClient
import client.schemaregistry.DeleteSchemaClientImpl
import dto.errors.HttpErrorDTO
import dto.schema.*
import domain.{Version, Versions}
import cats.data.EitherT
import cats.effect.{Async, Resource}

trait DeleteSchemaClient[F[_]] extends SchemaClient[F]:
  def deleteSchemaVersion(subject: String, version: Int): EitherT[F, HttpErrorDTO, Version]
  def deleteSchemaSubject(subject: String): EitherT[F, HttpErrorDTO, Versions]

object DeleteSchemaClient:
  def make[F[_]: Async](
      baseUri: String,
      httpClient: HttpClient[F]): Resource[F, DeleteSchemaClient[F]] =
    Resource.pure[F, DeleteSchemaClient[F]](DeleteSchemaClientImpl(baseUri, httpClient))
