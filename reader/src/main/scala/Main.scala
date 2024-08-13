package io.github.sergeiionin.contractsregistrator

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO, IOApp}
import fs2.kafka.{ConsumerSettings, Deserializer, KafkaConsumer}
import fs2.kafka.*
import handler.ContractsHandler
import repository.ContractsRepository

import io.circe.{Decoder, parser}
import io.circe
import domain.{Contract, SubjectAndVersion}
import github.GitClient

import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}
import org.http4s.ember.client.EmberClientBuilder
import skunk.Session
import natchez.Trace.Implicits.noop
import config.ApplicationConfig
import schemaregistry.KeyType.*

import schemaregistry.schemasconsumer.SchemasKafkaConsumerImpl

object Main extends IOApp:
  
  override def run(args: List[String]): IO[ExitCode] =
    val config = ApplicationConfig.load
    val contractConfig = config.contract
    val kafka = config.kafka
    val postgres = config.postgres
    
    val topics = List(kafka.schemasTopic)
    val consumerProps = kafka.consumerProps.toMap()
    
    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    def parseRecord[R : Decoder](record: Option[String]): Either[circe.Error, R] =
      record match
        case None => Left(circe.DecodingFailure("Record is empty", List.empty))
        case Some(r) => 
          parser.parse(r).flatMap(json => {
          json.as[R]
        })
    
    def toContract(recordRaw: Option[String]): Either[circe.Error, Contract] =
      parseRecord[Contract](recordRaw)
    
    def toSubjectAndVersion(recordRaw: Option[String]): Either[circe.Error, SubjectAndVersion] =
      parseRecord[SubjectAndVersion](recordRaw)
    
    given Deserializer[IO, Bytes] = Deserializer.lift(bs => IO.pure(Bytes(bs)))
    
    val consumerSettings =
      ConsumerSettings
        .apply(
          Deserializer.apply[IO, Bytes],
          Deserializer.apply[IO, Bytes]
        )
        .withProperties(consumerProps)
    
    (for
      session         <- Session.single[IO](host = postgres.host, port = postgres.port, user = postgres.user, database = postgres.database)
      repo            <- ContractsRepository.make[IO](session)
      client          <- EmberClientBuilder.default[IO].build
      consumer        <- KafkaConsumer.resource[IO, Bytes, Bytes](consumerSettings)
      schemasConsumer = SchemasKafkaConsumerImpl[IO](consumer)
      gitClient       <- GitClient.make[IO](contractConfig.owner, contractConfig.repo, contractConfig.path,
                                      contractConfig.baseBranch, client, Some(contractConfig.token))
      handler         <- ContractsHandler.make[IO](repo, gitClient)
    yield (schemasConsumer, handler)).use {
      case (sc, h) =>
        logger.info("Starting contracts registry") >>
        sc.subscribe(NonEmptyList.fromListUnsafe(topics)) >>
          logger.info(s"Subscribed to topics ${topics.mkString(", ")}") >>
          sc.stream()
            .evalMap(cr =>
              val keyType = sc.getRecordKeyType(Bytes.toString(cr.record.key))
              val recordOpt = Option(cr.record.value).map(bytes => Bytes.toString(bytes))
              logger.info(s"Received record: ${recordOpt.getOrElse("N/A")}") >> {
                keyType match
                  case SCHEMA =>
                    val contract = toContract(recordOpt)
                    contract.fold[IO[Unit]](
                      e => logger.error(s"Failed to parse contract: ${e.getMessage}"),
                      c => logger.info(s"Registering new contract: $c") >>
                        h.addContract(c)
                    )
                  case DELETE_SUBJECT => // todo in case of "soft-delete (DELETE /subjects/<subj>/versions/<ver> keytype is SCHEMA but there's a field "deleted" set to true)"
                    val subjectAndVersion = toSubjectAndVersion(recordOpt)
                    subjectAndVersion.fold[IO[Unit]](
                      e => logger.error(s"Failed to parse delete record: ${e.getMessage}"),
                      sv => logger.info(s"Deleting contract: ${sv.subject}:${sv.version}") >>
                        h.deleteContract(sv.subject, sv.version)
                    )
                  case NOOP =>
                    logger.info("NOOP record")
                  case UNKNOWN =>
                    logger.error("Unknown record type")  
              }
            )
            .compile
            .drain
    }.as(ExitCode.Success)
