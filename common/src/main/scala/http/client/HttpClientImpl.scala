package io.github.sergeiionin.contractsregistrator
package http.client

import cats.syntax.applicative.*
import cats.data.EitherT
import cats.effect.Async
import org.http4s.{Uri, Response}
import org.http4s.client.Client
import org.http4s.EntityEncoder
import dto.errors.HttpErrorDTO

final class HttpClientImpl[F[_] : Async](client: Client[F]) extends HttpClient[F]:
  import extensions.*
  
  def get(uri: Uri, token: Option[String]): EitherT[F, HttpErrorDTO, Response[F]] =
    client.run(ClientUtils.getRequest(uri, token))
      .use(resp => resp.pure[F])
      .toEitherT

  def post[T](uri: Uri, entity: T, token: Option[String])
                     (using EntityEncoder[F, T]): EitherT[F, HttpErrorDTO, Response[F]] =
      client.run(ClientUtils.postRequest(uri, token, entity))
        .use(resp => resp.pure[F])
        .toEitherT

  def delete(uri: Uri, token: Option[String]): EitherT[F, HttpErrorDTO, Response[F]] =
      client.run(ClientUtils.deleteRequest(uri, token))
        .use(resp => resp.pure[F])
        .toEitherT
