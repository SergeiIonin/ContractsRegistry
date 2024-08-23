package io.github.sergeiionin.contractsregistrator
package client

import http.client.HttpClient
import client.schemaregistry.SchemaRegistryClientImpl
import dto.errors.HttpErrorDTO
import dto.schema.*

import cats.data.EitherT
import cats.effect.{Async, Resource}
import org.http4s.ember.client.EmberClientBuilder

trait SchemasClient[F[_]]:
  def createSchema(subject: String, schemaDTO: CreateSchemaDTO): EitherT[F, HttpErrorDTO, CreateSchemaResponseDTO]
  def getSchemaVersion(subject: String, version: Int): EitherT[F, HttpErrorDTO, SchemaDTO]
  def getSchemaVersions(subject: String): EitherT[F, HttpErrorDTO, Versions]
  def getSubjects(): EitherT[F, HttpErrorDTO, Subjects]
  def getLatestSchema(subject: String): EitherT[F, HttpErrorDTO, SchemaDTO]
  def deleteSchemaVersion(subject: String, version: Int): EitherT[F, HttpErrorDTO, Version]
  def deleteSchemaSubject(subject: String): EitherT[F, HttpErrorDTO, Versions]

object SchemasClient:
  def make[F[_] : Async](baseUri: String, httpClient: HttpClient[F]): Resource[F, SchemasClient[F]] =
    Resource.pure[F, SchemasClient[F]](SchemaRegistryClientImpl(baseUri, httpClient))
