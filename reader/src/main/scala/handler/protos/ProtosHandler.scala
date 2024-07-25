package io.github.sergeiionin.contractsregistrator
package handler.protos

import cats.effect.{Sync, Resource}

trait ProtosHandler[F[_]]:
  def saveProto(name: String, content: String): F[Unit]

object ProtosHandler:
  def make[F[_] : Sync](path: String): Resource[F, ProtosHandler[F]] =
    Resource.pure[F, ProtosHandler[F]](ProtosHandlerImpl[F](path))

