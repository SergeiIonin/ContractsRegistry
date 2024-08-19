package io.github.sergeiionin.contractsregistrator
package http.client

import cats.Monad
import cats.syntax.applicative.*
import http.client.ContractsRegistryHttpClientTestImpl.IdResponse
import org.http4s.{Response, Uri}
import org.http4s.EntityEncoder
import io.circe.{Encoder, Decoder}
import org.http4s.circe.jsonEncoderOf

final class ContractsRegistryHttpClientTestImpl[F[_] : Monad]() extends ContractsRegistryHttpClient[F]:
  import ContractsRegistryHttpClientTestImpl.given
  
  def get(uri: Uri, token: Option[String]): F[Response[F]] =
    Response.apply().pure[F]

  def post[T](uri: Uri, entity: T, token: Option[String])(using EntityEncoder[F, T]): F[Response[F]] =
    Response.apply().withEntity(IdResponse(1)).pure[F]
    
  def delete(uri: Uri, token: Option[String]): F[Response[F]] =
    Response.apply().pure[F]

object ContractsRegistryHttpClientTestImpl:
  def make[F[_] : Monad](): ContractsRegistryHttpClientTestImpl[F] =
    new ContractsRegistryHttpClientTestImpl[F]()

  final case class IdResponse(id: Int) derives Encoder, Decoder

  given idResponseEncoder[F[_]]: EntityEncoder[F, IdResponse] = jsonEncoderOf[F, IdResponse]
