package io.github.sergeiionin.contractsregistrator
package handler.protos

import cats.effect.Sync
import java.nio.file.{Paths, Files}
import java.nio.charset.StandardCharsets
import cats.effect.Sync
import cats.implicits.*

class ProtosHandlerImpl[F[_] : Sync](path: String) extends ProtosHandler[F]:
  def saveProto(name: String, content: String): F[Unit] =
    val filePath = Paths.get(path, s"$name.proto")
    Sync[F].blocking {
      Files.write(filePath, content.getBytes(StandardCharsets.UTF_8))
    }.as(())