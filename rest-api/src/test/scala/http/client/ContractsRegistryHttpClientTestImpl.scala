package io.github.sergeiionin.contractsregistrator
package http.client

import cats.Monad
import cats.syntax.applicative.*
import org.http4s.{Response, Status, Uri}
import org.http4s.EntityEncoder
import io.circe.{Decoder, Encoder}
import org.http4s.circe.jsonEncoderOf
import domain.Contract
import dto.schemaregistry.{SchemaDTO, CreateSchemaResponseDTO}

import scala.collection.immutable.::
// todo use MockSchemaRegistryClient
/*import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient*/


final class ContractsRegistryHttpClientTestImpl[F[_] : Monad]() extends ContractsRegistryHttpClient[F]:
  import ContractsRegistryHttpClientTestImpl.given
  import ContractsRegistryHttpClientTestImpl.*
  import dto.schemaregistry.SchemaRegistryDTO.given

  private val storage = TestContractsStorage()
  
  def get(uri: Uri, token: Option[String]): F[Response[F]] =
    uri.renderString.split("/").toList match
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
        Response.apply().withStatus(Status.InternalServerError).pure[F]

  def post[T](uri: Uri, entity: T, token: Option[String])(using EntityEncoder[F, T]): F[Response[F]] =
    uri.renderString.split("/").toList match
      case _ :: _ :: _ :: "subjects" :: subject :: "versions" :: Nil =>
        val resp = storage.add(subject, entity.asInstanceOf[SchemaDTO])
        Response.apply().withEntity(CreateSchemaResponseDTO(resp)).pure[F]
      case _ =>
        Response.apply().withStatus(Status.InternalServerError).pure[F]

    
  def delete(uri: Uri, token: Option[String]): F[Response[F]] =
    uri.renderString.split("/").toList match
      case _ :: _ :: _ :: "subjects" :: subject :: "versions" :: version :: Nil =>
        storage.delete(subject, version.toInt) match
          case Left(_) => Response.apply().withStatus(Status.NotFound).pure[F]
          case Right(v) => 
            val ver = v
            Response.apply().withEntity(ver).pure[F]
      case _ :: _ :: _ :: "subjects" :: subject :: Nil =>
        storage.deleteSubject(subject) match
          case Left(_) => Response.apply().withStatus(Status.NotFound).pure[F]
          case Right(vers) =>
            Response.apply().withEntity(vers).pure[F]
      case _ => Response.apply().withStatus(Status.InternalServerError).pure[F]    

object ContractsRegistryHttpClientTestImpl:
  def make[F[_] : Monad](): ContractsRegistryHttpClientTestImpl[F] =
    new ContractsRegistryHttpClientTestImpl[F]()

  given intEncoder[F[_]]: EntityEncoder[F, Int] = jsonEncoderOf[F, Int]
  given intsEncoder[F[_]]: EntityEncoder[F, List[Int]] = jsonEncoderOf[F, List[Int]]
  given stringsEncoder[F[_]]: EntityEncoder[F, List[String]] = jsonEncoderOf[F, List[String]]
  given contractEncoder[F[_]]: EntityEncoder[F, Contract] = jsonEncoderOf[F, Contract] // fixme should be ContractDTO
