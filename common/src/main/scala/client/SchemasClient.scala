package io.github.sergeiionin.contractsregistrator
package client

import cats.data.EitherT
import cats.effect.{Async, Resource}
import org.http4s.ember.client.EmberClientBuilder
import client.schemaregistry.SchemaRegistryClientImpl
import dto.schema.{CreateSchemaDTO, CreateSchemaResponseDTO, SchemaDTO, Subjects, Version, Versions}

import io.github.sergeiionin.contractsregistrator.dto.errors.HttpErrorDTO


trait SchemasClient[F[_]]:
  def createSchema(subject: String, schemaDTO: CreateSchemaDTO): EitherT[F, HttpErrorDTO, CreateSchemaResponseDTO]
  def getSchemaVersion(subject: String, version: Int): EitherT[F, HttpErrorDTO, SchemaDTO]
  def getSchemaVersions(subject: String): EitherT[F, HttpErrorDTO, Versions]
  def getSubjects(): EitherT[F, HttpErrorDTO, Subjects]
  def getLatestVersion(subject: String): EitherT[F, HttpErrorDTO, SchemaDTO]
  def deleteSchemaVersion(subject: String, version: Int): EitherT[F, HttpErrorDTO, Version]
  def deleteSchemaSubject(subject: String): EitherT[F, HttpErrorDTO, Versions]

object SchemasClient:
  def make[F[_] : Async](baseUri: String): Resource[F, SchemasClient[F]] =
    EmberClientBuilder.default[F].build.map(client => SchemaRegistryClientImpl(baseUri, client))