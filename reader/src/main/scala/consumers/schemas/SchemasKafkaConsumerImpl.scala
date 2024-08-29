package io.github.sergeiionin.contractsregistrator
package consumers.schemas

import cats.syntax.option.*
import cats.syntax.flatMap.*
import cats.syntax.applicative.*
import cats.data.NonEmptyList
import cats.effect.{Async, Resource}
import fs2.kafka.{CommittableConsumerRecord, ConsumerSettings, Deserializer, KafkaConsumer}
import io.circe.{parser, Error as circeError}
import consumers.KafkaEventsConsumer
import org.typelevel.log4cats.Logger
import KeyType.*
import producer.EventsProducer
import consumers.Consumer
import domain.events.contracts.{ContractCreateRequestedKey, ContractCreateRequested}
import domain.{Contract, SubjectAndVersion}
import circe.codecs.domain.ContractCodec.given
import circe.codecs.domain.SubjectAndVersionCodec.given
import io.circe.Error as CirceError

final class SchemasKafkaConsumerImpl[F[_] : Async : Logger](
                                                    topics: NonEmptyList[String],
                                                    kafkaConsumer: KafkaConsumer[F, Bytes, Bytes],
                                                    contractsProducer: EventsProducer[F, ContractCreateRequestedKey, ContractCreateRequested]       
                                                  ) extends KafkaEventsConsumer[F, Bytes, Bytes](kafkaConsumer):
  import circe.parseRaw
  
  private val logger = summon[Logger[F]]
  
  def getRecordKeyType(keyRaw: String): KeyType =
    parser.parse(keyRaw).flatMap(json => {
      json.hcursor.downField("keytype").as[String] // fixme "keytype" should be constant
    }).map(KeyType.fromString).getOrElse(OTHER)

  override def process(): F[Unit] =
    subscribe(topics) >>
      kafkaConsumer
        .stream
        .evalMap(cr =>
          val key = Bytes.toString(cr.record.key)
          val keyType = getRecordKeyType(key)
          val recordOpt = Option(cr.record.value).map(bytes => Bytes.toString(bytes))
          keyType match
            case SCHEMA | DELETE_SUBJECT | NOOP =>
              processSchemaRegistryRecord(keyType, recordOpt)
            case OTHER =>
              logger.info(s"Other record type (not a _schema topic record)")
        )
        .compile
        .drain
  
  private def createContractVersion(contract: Contract): F[Unit] =
      contractsProducer
        .produce(
          ContractCreateRequestedKey(contract.subject, contract.version),
          ContractCreateRequested(contract)
        )
  
  private def toContract(recordRaw: Option[String]): Either[CirceError, Contract] =
    parseRaw[Contract](recordRaw)
  
  def processSchemaRegistryRecord(
                                  keyType: KeyType,
                                  recordOpt: Option[String],
                                  ): F[Unit] =
    logger.info(s"Received record: ${recordOpt.getOrElse("N/A")}") >> {
      keyType match
        case SCHEMA =>
          val contract = toContract(recordOpt)
          contract match
            case Left(e) =>
              logger.error(s"Failed to parse contract: ${e.getMessage}")
            case Right(c) =>
              logger.info(s"Registering new contract: ${c.subject}:${c.version}") >>
                createContractVersion(c)
        case NOOP =>
          logger.info("NOOP record")
        case OTHER =>
          logger.error("Other record type (not a _schema topic record)")
    }

object SchemasKafkaConsumerImpl:
  def make[F[_] : Async : Logger](
                                   topics: NonEmptyList[String],
                                   consumerSettings: ConsumerSettings[F, Bytes, Bytes],
                                   contractsProducer: EventsProducer[F, ContractCreateRequestedKey, ContractCreateRequested]
                                 ): Resource[F, Consumer[F]] =
    KafkaConsumer.resource[F, Bytes, Bytes](consumerSettings)
      .map(consumer => SchemasKafkaConsumerImpl[F](topics, consumer, contractsProducer))