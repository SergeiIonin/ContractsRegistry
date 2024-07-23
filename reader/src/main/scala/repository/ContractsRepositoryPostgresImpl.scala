package io.github.sergeiionin.contractsregistrator
package repository

import cats.Monad
import cats.syntax.monad.*
import cats.syntax.functor.*
import cats.effect.Resource
import domain.Contract
import skunk.{Encoder, Decoder, Command, Query}
import skunk.*
import skunk.Session
import skunk.implicits.*
import skunk.codec.all.*

import io.circe.syntax.given 
import skunk.circe.codec.json.jsonb

class ContractsRepositoryPostgresImpl[F[_] : Monad](session: Resource[F, Session[F]]) extends ContractsRepository[F]:
  val contractEncoder: skunk.Encoder[Contract] =
    (varchar ~ varchar.opt ~ jsonb).contramap {
      case Contract(name, description, fields) =>
        name ~ description ~ fields.asJson
    }

  val contractDecoder: skunk.Decoder[Contract] =
    (varchar ~ varchar.opt ~ jsonb).map {
      case name ~ description ~ fieldsJson =>
        Contract(name, description, fieldsJson.as[Map[String, Any]].getOrElse(Map.empty))
    }

  val insertCommand: Command[Contract] =
    sql"INSERT INTO contracts (name, description, fields) VALUES ($contractEncoder)".command

  val selectByNameQuery: Query[String, Contract] =
    sql"SELECT name, description, fields FROM contracts WHERE name = $varchar".query(contractDecoder)

  val selectAllQuery: Query[Void, Contract] =
    sql"SELECT name, description, fields FROM contracts".query(contractDecoder)

  val deleteCommand: Command[String] =
    sql"DELETE FROM contracts WHERE name = $varchar".command

  override def save(contract: Contract): F[Unit] =
    session.use(_.prepare(insertCommand).map(_.execute(contract))).void

  override def get(name: String): F[Option[Contract]] =
    session.use(_.prepare(selectByNameQuery).map(_.option(name)))

  override def getAll(): F[fs2.Stream[F, Contract]] =
    session.use(_.stream(selectAllQuery)(Void, 10))

  override def delete(name: String): F[Unit] =
    session.use(_.prepare(deleteCommand).map(_.execute(name))).void