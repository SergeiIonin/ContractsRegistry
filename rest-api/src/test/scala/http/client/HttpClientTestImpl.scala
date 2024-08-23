package io.github.sergeiionin.contractsregistrator
package http.client

import cats.Monad
import cats.syntax.applicative.*
import org.http4s.{Response, Status, Uri}
import org.http4s.EntityEncoder
import io.circe.{Decoder, Encoder}
import org.http4s.circe.jsonEncoderOf
import domain.Contract
import dto.schema.{CreateSchemaDTO, CreateSchemaResponseDTO}

import cats.data.EitherT
import io.github.sergeiionin.contractsregistrator.dto.errors.HttpErrorDTO

import scala.collection.immutable.::
// todo use MockSchemaRegistryClient
/*import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient*/


final class HttpClientTestImpl[F[_] : Monad]() extends HttpClient[F]:
  import HttpClientTestImpl.given
  import http4s.entitycodecs.CreateSchemaDtoEntityCodec.given
  import http4s.entitycodecs.CreateSchemaResponseDtoEntityCodec.given
  import http4s.entitycodecs.SchemaDtoEntityCodec.given
  import http.client.HttpClient.*

  private val storage = TestSchemaStorage()
  
  def get(uri: Uri, token: Option[String]): EitherT[F, HttpErrorDTO, Response[F]] =
    (uri.renderString.split("/").toList match
      case _ :: _ :: _ :: "subjects" :: subject :: "versions" :: version :: Nil =>
        storage.get(subject, version.toInt) match
          case Left(_) => Response.apply().withStatus(Status.NotFound).pure[F]
          case Right(contract) => Response.apply().withEntity(contract).pure[F]
      case _ :: _ :: _ :: "subjects" :: subject :: "versions" :: Nil =>
        storage.getVersions(subject) match
          case Left(_) => Response.apply().withStatus(Status.NotFound).pure[F]
          case Right(versions) => Response.apply().withEntity(versions).pure[F]
      case _ :: _ :: _ :: "subjects" :: Nil =>
        Response.apply().withEntity(storage.getSubjects).pure[F]
      case _ =>
        Response.apply().withStatus(Status.InternalServerError).pure[F])
      .toEitherT

  def post[T](uri: Uri, entity: T, token: Option[String])
             (using EntityEncoder[F, T]): EitherT[F, HttpErrorDTO, Response[F]] =
    (uri.renderString.split("/").toList match
      case _ :: _ :: _ :: "subjects" :: subject :: "versions" :: Nil => {
        val resp = storage.add(subject, entity.asInstanceOf[CreateSchemaDTO])
        Response.apply().withEntity(CreateSchemaResponseDTO(resp)).pure[F]
      }
      case _ =>
        Response.apply().withStatus(Status.InternalServerError).pure[F])
      .toEitherT

    
  def delete(uri: Uri, token: Option[String]): EitherT[F, HttpErrorDTO, Response[F]] =
    (uri.renderString.split("/").toList match
      case _ :: _ :: _ :: "subjects" :: subject :: "versions" :: version :: Nil =>
        storage.delete(subject, version.toInt) match
          case Left(_) => Response.apply().withStatus(Status.NotFound).pure[F]
          case Right(v) => {
            val ver = v
            Response.apply().withEntity(ver).pure[F]
          }
      case _ :: _ :: _ :: "subjects" :: subject :: Nil =>
        storage.deleteSubject(subject) match
          case Left(_) => Response.apply().withStatus(Status.NotFound).pure[F]
          case Right(vers) =>
            Response.apply().withEntity(vers).pure[F]
      case _ => Response.apply().withStatus(Status.InternalServerError).pure[F])
      .toEitherT

object HttpClientTestImpl:
  def make[F[_] : Monad](): HttpClientTestImpl[F] =
    new HttpClientTestImpl[F]()

  given intEncoder[F[_]]: EntityEncoder[F, Int] = jsonEncoderOf[F, Int]
  given intsEncoder[F[_]]: EntityEncoder[F, List[Int]] = jsonEncoderOf[F, List[Int]]
  given stringsEncoder[F[_]]: EntityEncoder[F, List[String]] = jsonEncoderOf[F, List[String]]
