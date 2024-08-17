package io.github.sergeiionin.contractsregistrator
package http.client

import cats.Monad
import cats.syntax.applicative.*
import org.http4s.{Response, Uri}

final class ContractsRegistryHttpClientTestImpl[F[_] : Monad]() extends ContractsRegistryHttpClient[F]:
  def get(uri: Uri, token: Option[String]): F[Response[F]] =
    Response.apply().pure[F]

  def post[T](uri: Uri, entity: T, token: Option[String])(using EntityEncoder[F, T]): F[Response[F]] =
    Response.apply().pure[F]
    
  def delete(uri: Uri, token: Option[String]): F[Response[F]] =
    Response.apply().pure[F]

object ContractsRegistryHttpClientTestImpl:
  def make[F[_] : Monad](): ContractsRegistryHttpClientTestImpl[F] =
    new ContractsRegistryHttpClientTestImpl[F]()