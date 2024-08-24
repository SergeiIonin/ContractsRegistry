package io.github.sergeiionin.contractsregistrator
package http.client

import cats.data.EitherT
import cats.effect.{Async, Resource}
import org.http4s.{Response, Uri}
import org.http4s.EntityEncoder
import org.http4s.ember.client.EmberClientBuilder
import dto.errors.HttpErrorDTO

import cats.{Applicative, Functor}

trait HttpClient[F[_]]:
  def get(uri: Uri, token: Option[String]): EitherT[F, HttpErrorDTO, Response[F]]

  def post[T](uri: Uri, entity: T, token: Option[String])
             (using EntityEncoder[F, T]): EitherT[F, HttpErrorDTO, Response[F]]

  def delete(uri: Uri, token: Option[String]): EitherT[F, HttpErrorDTO, Response[F]]

object HttpClient:
  def make[F[_] : Async](): Resource[F, HttpClient[F]] =
    EmberClientBuilder.default[F].build.map(HttpClientImpl(_))
