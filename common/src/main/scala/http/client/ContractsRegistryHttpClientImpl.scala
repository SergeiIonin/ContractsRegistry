package io.github.sergeiionin.contractsregistrator
package http.client

import cats.effect.Async
import cats.syntax.applicative.*
import org.http4s.client.Client
import org.http4s.{EntityEncoder, Response, Uri}

class ContractsRegistryHttpClientImpl[F[_] : Async](client: Client[F]) extends ContractsRegistryHttpClient[F]:
  def get(uri: Uri, token: Option[String]): F[Response[F]] =
    client.run(ClientUtils.getRequest(uri, token))
      .use(resp => resp.pure[F])
  
  def post[T](uri: Uri, entity: T, token: Option[String])(using EntityEncoder[F, T]): F[Response[F]] = 
    client.run(ClientUtils.postRequest(uri, token, entity))
      .use(resp => resp.pure[F])
