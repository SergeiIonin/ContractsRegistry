package io.github.sergeiionin.contractsregistrator
package http.client

import cats.{Applicative, Functor}
import cats.data.EitherT
import dto.errors.HttpErrorDTO

package object extensions:
  extension [F[_] : Functor, R](r: F[R])
    def toEitherT: EitherT[F, HttpErrorDTO, R] =
      EitherT.liftF[F, HttpErrorDTO, R](r)

  extension [F[_] : Applicative](err: HttpErrorDTO)
    def toLeftEitherT[R]: EitherT[F, HttpErrorDTO, R] =
      EitherT.leftT[F, R](err)