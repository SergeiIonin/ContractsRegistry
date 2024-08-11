package io.github.sergeiionin.contractsregistrator
package http.client

import cats.effect.Async
import cats.effect.kernel.Resource
import org.http4s.{EntityEncoder, Response, Uri}
import org.http4s.ember.client.EmberClientBuilder

trait ContractsRegistryHttpClient[F[_]]:
  def get(uri: Uri, token: Option[String]): F[Response[F]]
  def post[T](uri: Uri, entity: T, token: Option[String])(using EntityEncoder[F, T]): F[Response[F]]
  
object ContractsRegistryHttpClient:
  def make[F[_] : Async](): Resource[F, ContractsRegistryHttpClient[F]] =
    EmberClientBuilder.default[F].build.map(ContractsRegistryHttpClientImpl(_))
    