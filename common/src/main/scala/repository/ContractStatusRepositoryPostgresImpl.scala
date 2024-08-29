package io.github.sergeiionin.contractsregistrator
package repository

import cats.effect.{Async, Resource}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.monad.*
import org.typelevel.log4cats.Logger
import skunk.circe.codec.json.jsonb
import skunk.codec.all.*
import skunk.implicits.*
import skunk.{Command, Session, *}

class ContractStatusRepositoryPostgresImpl[F[_] : Async : Logger](
                                                                   sessionR: Resource[F, Session[F]]
                                                                 ) extends ContractStatusRepository[F]:
  private val updateIsMergedCommand: Command[(String, Int)] =
    sql"UPDATE contracts SET isMerged = true WHERE subject = $varchar AND version = $int4".command
  
  override def updateIsMerged(subject: String, version: Int): F[Unit] = 
    sessionR.use { session =>
      session.prepare(updateIsMergedCommand).flatMap(_.execute((subject, version))).void
    }
