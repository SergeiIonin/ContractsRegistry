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

class ContractsRepositoryPostgresImpl[F[_] : Async](session: Session[F])(using Logger[F]) extends ContractsRepository[F]:
  private val contractEncoder: skunk.Encoder[Contract] =
    (varchar ~ int4 ~ int4 ~ text).contramap {
      case Contract(subject, version, id, schema, _) =>
        subject ~ version ~ id ~ schema
    }

  private val contractDecoder: skunk.Decoder[Contract] =
    (varchar ~ int4 ~ int4 ~ text).map {
      case subject ~ version ~ id ~ schema =>
        Contract(subject, version, id, schema)
    }

  private val insertCommand: Command[Contract] =
    sql"INSERT INTO contracts (subject, version, id, schema) VALUES ($contractEncoder)".command

  private val selectBySubjectAndVersionQuery: Query[(String, Int), Contract] =
    sql"SELECT subject, version, id, schema FROM contracts WHERE subject = $varchar AND version = $int4".query(contractDecoder)

  private val selectAllQuery: Query[Void, Contract] =
    sql"SELECT subject, version, id, schema FROM contracts".query(contractDecoder)

  private val deleteCommand: Command[(String, Int)] =
    sql"DELETE FROM contracts WHERE subject = $varchar AND version = $int4".command

  // fixme id should be subject:version
  override def save(contract: Contract): F[Unit] =
    session.prepare(insertCommand).flatMap(_.execute(contract))
      .recoverWith {
        case SqlState.UniqueViolation(_) => 
          summon[Logger[F]].info(s"contract with ${contract.subject}:${contract.id} already exists")
            .as(skunk.data.Completion.Insert(0))
      }
      .void

  override def get(subject: String, version: Int): F[Option[Contract]] =
    session.prepare(selectBySubjectAndVersionQuery).flatMap(_.option((subject, version)))

  override def getAll(): F[fs2.Stream[F, Contract]] =
    session.stream(selectAllQuery)(Void, 10).pure[F]

  override def delete(subject: String, version: Int): F[Unit] =
    session.prepare(deleteCommand).flatMap(_.execute((subject, version))).void