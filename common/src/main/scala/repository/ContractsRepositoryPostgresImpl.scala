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
import domain.SchemaType

import cats.effect.kernel.Async
import org.typelevel.log4cats.Logger

class ContractsRepositoryPostgresImpl[F[_] : Async](sessionR: Resource[F, Session[F]])(using Logger[F]) extends ContractsRepository[F]:
  private val contractEncoder: skunk.Encoder[Contract] =
    (varchar ~ int4 ~ int4 ~ text ~ text ~ bool).contramap {
      case Contract(subject, version, id, schema, schemaType, isMerged, _) =>
        subject ~ version ~ id ~ schema ~ schemaType.toString ~ isMerged
    }

  private val contractDecoder: skunk.Decoder[Contract] =
    (varchar ~ int4 ~ int4 ~ text ~ text ~ bool).map {
      case subject ~ version ~ id ~ schema ~ schemaType ~ isMerged =>
        Contract(subject, version, id, schema, SchemaType.fromString(schemaType), isMerged)
    }

  private val versionDecoder: skunk.Decoder[Int] = int4
  
  private val subjectDecoder: skunk.Decoder[String] = varchar

  private val insertCommand: Command[Contract] =
    sql"INSERT INTO contracts (subject, version, id, schema, schemaType, isMerged) VALUES ($contractEncoder)".command

  private val updateIsMergedCommand: Command[(String, Int)] =
    sql"UPDATE contracts SET isMerged = true WHERE subject = $varchar AND version = $int4".command
  
  private val selectBySubjectAndVersionQuery: Query[(String, Int), Contract] =
    sql"SELECT subject, version, id, schema, schemaType, isMerged FROM contracts WHERE subject = $varchar AND version = $int4".query(contractDecoder)

  private val selectAllQuery: Query[Void, Contract] =
    sql"SELECT subject, version, id, schema, schemaType, isMerged FROM contracts".query(contractDecoder)

  private val selectAllSubjectsQuery: Query[Void, String] =
    sql"SELECT DISTINCT subject FROM contracts".query(subjectDecoder)

  private val selectAllVersionsForSubjectQuery: Query[String, Int] =
    sql"SELECT version FROM contracts WHERE subject = $varchar".query(versionDecoder)

  private val selectLatestContractQuery: Query[String, Contract] =
    sql"SELECT subject, version, id, schema, schemaType, isMerged FROM contracts WHERE subject = $varchar ORDER BY version DESC LIMIT 1".query(contractDecoder)
  
  private val deleteSubjectAndVersionCommand: Command[(String, Int)] =
    sql"DELETE FROM contracts WHERE subject = $varchar AND version = $int4".command

  private val deleteSubjectCommand: Command[String] =
    sql"DELETE FROM contracts WHERE subject = $varchar".command

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

  override def updateIsMerged(subject: String, version: Int): F[Unit] = 
    sessionR.use { session =>
      session.prepare(updateIsMergedCommand).flatMap(_.execute((subject, version))).void
    }
  
  override def get(subject: String, version: Int): F[Option[Contract]] =
    sessionR.use { session =>
      session.prepare(selectBySubjectAndVersionQuery).flatMap(_.option((subject, version)))
    }

  override def getAll(): F[fs2.Stream[F, Contract]] =
    sessionR.use { session =>
      session.stream(selectAllQuery)(Void, 10).pure[F]
    }

  override def getAllSubjects(): F[fs2.Stream[F, String]] =
    sessionR.use { session =>
      session.stream(selectAllSubjectsQuery)(Void, 10).pure[F]
    }
  
  override def getAllVersionsForSubject(subject: String): F[fs2.Stream[F, Int]] =
    sessionR.use { session =>
      session.stream(selectAllVersionsForSubjectQuery)(subject, 10).pure[F]
    }

  override def getLatestContract(subject: String): F[Option[Contract]] =
    sessionR.use { session =>
      session.prepare(selectLatestContractQuery).flatMap(_.option(subject))
    }
  
  override def delete(subject: String, version: Int): F[Unit] =
    sessionR.use { session =>
      session.prepare(deleteSubjectAndVersionCommand).flatMap(_.execute((subject, version))).void
    }

  override def deleteSubject(subject: String): F[Unit] =
    sessionR.use { session =>
      session.prepare(deleteSubjectCommand).flatMap(_.execute(subject)).void
    }
