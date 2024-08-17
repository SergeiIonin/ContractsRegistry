package io.github.sergeiionin.contractsregistrator
package repository

import cats.Monad
import cats.syntax.monad.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.applicative.*
import cats.syntax.monadError.*
import cats.syntax.applicativeError.*
import cats.effect.Resource
import domain.Contract

import skunk.{Command, Decoder, Encoder, Query}
import skunk.*
import skunk.Session
import skunk.implicits.*
import skunk.codec.all.*
import io.circe.syntax.given
import skunk.circe.codec.json.jsonb
import domain.Contract.given

import cats.effect.kernel.Async
import org.typelevel.log4cats.Logger

class ContractsRepositoryPostgresImpl[F[_] : Async](sessionR: Resource[F, Session[F]])(using Logger[F]) extends ContractsRepository[F]:
  private val contractEncoder: skunk.Encoder[Contract] =
    (varchar ~ int4 ~ int4 ~ text ~ bool).contramap {
      case Contract(subject, version, id, schema, isMerged, _) =>
        subject ~ version ~ id ~ schema ~ isMerged
    }

  private val contractDecoder: skunk.Decoder[Contract] =
    (varchar ~ int4 ~ int4 ~ text ~ bool).map {
      case subject ~ version ~ id ~ schema ~ isMerged =>
        Contract(subject, version, id, schema, isMerged)
    }

  private val subjectAndVersionDecoder: skunk.Decoder[Int] = int4

  private val insertCommand: Command[Contract] =
    sql"INSERT INTO contracts (subject, version, id, schema, isMerged) VALUES ($contractEncoder)".command

  private val selectBySubjectAndVersionQuery: Query[(String, Int), Contract] =
    sql"SELECT subject, version, id, schema, isMerged FROM contracts WHERE subject = $varchar AND version = $int4".query(contractDecoder)

  private val selectAllQuery: Query[Void, Contract] =
    sql"SELECT subject, version, id, schema, isMerged FROM contracts".query(contractDecoder)

  private val selectAllVersionsForSubjectQuery: Query[String, Int] =
    sql"SELECT version FROM contracts WHERE subject = $varchar".query(subjectAndVersionDecoder)

  private val deleteCommand: Command[(String, Int)] =
    sql"DELETE FROM contracts WHERE subject = $varchar AND version = $int4".command

  override def save(contract: Contract): F[Unit] =
    sessionR.use { session =>
      session.prepare(insertCommand).flatMap(_.execute(contract))
        .recoverWith {
          case SqlState.UniqueViolation(_) => 
            summon[Logger[F]].info(s"contract with ${contract.subject}:${contract.version} already exists")
              .as(skunk.data.Completion.Insert(0))
        }
        .void
    }

  override def get(subject: String, version: Int): F[Option[Contract]] =
    sessionR.use { session =>
      session.prepare(selectBySubjectAndVersionQuery).flatMap(_.option((subject, version)))
    }

  override def getAll(): F[fs2.Stream[F, Contract]] =
    sessionR.use { session =>
      session.stream(selectAllQuery)(Void, 10).pure[F]
    }

  override def getAllVersionsForSubject(subject: String): F[fs2.Stream[F, Int]] =
    sessionR.use { session =>
      session.stream(selectAllVersionsForSubjectQuery)(subject, 10).pure[F]
    }

  override def delete(subject: String, version: Int): F[Unit] =
    sessionR.use { session =>
      session.prepare(deleteCommand).flatMap(_.execute((subject, version))).void
    }
