package io.github.sergeiionin.contractsregistrator
package client

import http.client.HttpClient
import client.schemaregistry.CreateSchemaClientImpl
import dto.errors.HttpErrorDTO
import dto.schema.*
import cats.data.EitherT
import cats.effect.{Async, Resource}

trait CreateSchemaClient[F[_]] extends SchemaClient[F]:
  def createSchema(subject: String, schemaDTO: CreateSchemaDTO): EitherT[F, HttpErrorDTO, CreateSchemaResponseDTO]

object CreateSchemaClient:
  def make[F[_] : Async](baseUri: String, httpClient: HttpClient[F]): Resource[F, CreateSchemaClient[F]] =
    Resource.pure[F, CreateSchemaClient[F]](CreateSchemaClientImpl(baseUri, httpClient))
