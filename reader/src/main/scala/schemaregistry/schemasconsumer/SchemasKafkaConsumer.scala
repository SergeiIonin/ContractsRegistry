package io.github.sergeiionin.contractsregistrator
package schemaregistry.schemasconsumer

import cats.data.NonEmptyList
import fs2.kafka.KafkaConsumer
import cats.data.NonEmptyList
import cats.effect.kernel.{Async, Resource}
import fs2.kafka.CommittableConsumerRecord
import io.circe.{parser, Error as circeError}
import schemaregistry.KeyType
import schemaregistry.KeyType.given

trait SchemasConsumer[F[_]]:
  def subscribe(topics: NonEmptyList[String]): F[Unit]
  def stream(): fs2.Stream[F, CommittableConsumerRecord[F, Bytes, Bytes]]
  def getRecordKeyType(keyRaw: String): KeyType =
    parser.parse(keyRaw).flatMap(json => {
      json.hcursor.downField("keytype").as[String] // fixme "keytype" should be constant
    }).map(KeyType.fromString).getOrElse(KeyType.OTHER)
    
object SchemasConsumer
  def make[F[_] : Async](kafkaConsumer: KafkaConsumer[F, Bytes, Bytes]): Resource[F, SchemasConsumer[F]] =
    Resource.pure[F, SchemasConsumer[F]](SchemasKafkaConsumerImpl[F](kafkaConsumer))
