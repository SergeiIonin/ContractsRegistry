package io.github.sergeiionin.contractsregistrator
package client.schemaregistry

import dto.errors.{BadRequestDTO, HttpErrorDTO, InternalServerErrorDTO}

import cats.effect.Async
import cats.data.EitherT
import cats.syntax.either.*
import http.client.extensions.*
import org.http4s.{EntityDecoder, Response}

trait ResponseMixin[F[_] : Async]:
  private def responseOk(response: Response[F]): Boolean =
    response.status.code >= 200 && response.status.code < 300

  private def responseBad(response: Response[F]): Boolean =
    response.status.code >= 400 && response.status.code < 500

  def convertResponse[R](response: Response[F])(errorMsg: => String)
                                (using EntityDecoder[F, R]): EitherT[F, HttpErrorDTO, R] =
    if (responseOk(response)) {
      response.as[R].toEitherT
    } else if (responseBad(response)) {
      BadRequestDTO(response.status.code, errorMsg).toLeftEitherT[R]
    } else {
      InternalServerErrorDTO().toLeftEitherT[R]
    }
