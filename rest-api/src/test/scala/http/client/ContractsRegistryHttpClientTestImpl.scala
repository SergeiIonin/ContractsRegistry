package io.github.sergeiionin.contractsregistrator
package http.client

import cats.Monad
import cats.syntax.applicative.*
import org.http4s.{Response, Status, Uri}
import org.http4s.EntityEncoder
import io.circe.{Decoder, Encoder}
import org.http4s.circe.jsonEncoderOf
import domain.Contract
import dto.ContractDTO

import scala.collection.immutable.::
// todo use MockSchemaRegistryClient
/*import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient
import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient*/


final class ContractsRegistryHttpClientTestImpl[F[_] : Monad]() extends ContractsRegistryHttpClient[F]:
  import ContractsRegistryHttpClientTestImpl.given
  import ContractsRegistryHttpClientTestImpl.*

  private val storage = TestContractsStorage()
  
  def get(uri: Uri, token: Option[String]): F[Response[F]] =
    uri.renderString.split("/").toList match
      case _ :: _ :: _ :: "subjects" :: subject :: "versions" :: version :: Nil =>
        storage.get(subject, version.toInt) match
          case Left(_) => Response.apply().withStatus(Status.NotFound).pure[F]
          case Right(contract) => Response.apply().withEntity(contract).pure[F]
      case _ =>
        Response.apply().withStatus(Status.InternalServerError).pure[F]

  def post[T](uri: Uri, entity: T, token: Option[String])(using EntityEncoder[F, T]): F[Response[F]] =
    uri.renderString.split("/").toList match
      case _ :: _ :: _ :: "subjects" :: subject :: "versions" :: Nil =>
        val resp = storage.add(subject, entity.asInstanceOf[ContractDTO])
        Response.apply().withEntity(IdResponse(resp)).pure[F]
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

  final case class IdResponse(id: Int) derives Encoder, Decoder

  given intEncoder[F[_]]: EntityEncoder[F, Int] = jsonEncoderOf[F, Int]
  given intsEncoder[F[_]]: EntityEncoder[F, List[Int]] = jsonEncoderOf[F, List[Int]]
  given idResponseEncoder[F[_]]: EntityEncoder[F, IdResponse] = jsonEncoderOf[F, IdResponse]
  given contractEncoder[F[_]]: EntityEncoder[F, Contract] = jsonEncoderOf[F, Contract]
