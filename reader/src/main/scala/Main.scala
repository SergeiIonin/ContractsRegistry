package io.github.sergeiionin.contractsregistrator

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO, IOApp}
import fs2.kafka.{ConsumerSettings, Deserializer, KafkaConsumer}
import cats.syntax.option.*
import handler.ContractsHandler
import repository.ContractsRepository
import producer.contracts.ContractCreateEventsKafkaProducer
import domain.events.prs.{PrClosedKey, PrClosed}
import config.ApplicationConfig
import io.circe.{Decoder, parser}
import io.circe
import io.circe.syntax.given
import domain.{Contract, SubjectAndVersion}
import github.GitHubClient
import consumers.schemas.KeyType.*
import consumers.schemas.KeyType
import consumers.schemas.SchemasKafkaConsumerImpl
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}
import org.http4s.ember.client.EmberClientBuilder
import skunk.Session
import natchez.Trace.Implicits.noop
import producer.EventsKafkaProducer.given

object Main extends IOApp:
  
  override def run(args: List[String]): IO[ExitCode] =
    val config = ApplicationConfig.load
    val contractConfig = config.contract
    val kafka = config.kafka
    val postgres = config.postgres
    
    val topics = List(kafka.schemasTopic, kafka.prsTopic)
    val consumerProps = kafka.consumerProps.toMap()
    
    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    def parseRaw[R : Decoder](raw: Option[String]): Either[circe.Error, R] =
      raw match
        case None => Left(circe.DecodingFailure("Record is empty", List.empty))
        case Some(r) => 
          parser.parse(r).flatMap(json => {
          json.as[R]
        })
    
    def toContract(recordRaw: Option[String]): Either[circe.Error, Contract] =
      parseRaw[Contract](recordRaw)
    
    def toSubjectAndVersion(recordRaw: Option[String]): Either[circe.Error, SubjectAndVersion] =
      parseRaw[SubjectAndVersion](recordRaw)
    
    given Deserializer[IO, Bytes] = Deserializer.lift(bs => IO.pure(Bytes(bs)))
    
    val consumerSettings =
      ConsumerSettings
        .apply(
          Deserializer.apply[IO, Bytes],
          Deserializer.apply[IO, Bytes]
        )
        .withProperties(consumerProps)
    
    def processSchemaRegistryRecord(keyType: KeyType,
                                    recordOpt: Option[String],
                                    handler: ContractsHandler[IO]): IO[Unit] =
      logger.info(s"Received record: ${recordOpt.getOrElse("N/A")}") >> {
        keyType match
          case SCHEMA =>
            val contract = toContract(recordOpt)
            contract match
              case Left(e) =>
                logger.error(s"Failed to parse contract: ${e.getMessage}")
              case Right(c) if c.deleted.contains(true) =>
                logger.info(s"Deleting contract's version: ${c.subject}:${c.version}") >>
                  handler.deleteContractVersion(c.subject, c.version)
              case Right(c) =>
                logger.info(s"Registering new contract: ${c.subject}:${c.version}") >>
                  handler.addContract(c)
          case DELETE_SUBJECT =>
            val subjectAndVersion = toSubjectAndVersion(recordOpt)
            subjectAndVersion.fold[IO[Unit]](
              e => logger.error(s"Failed to parse delete record: ${e.getMessage}"),
              sv => logger.info(s"Deleting contract: ${sv.subject}:${sv.version}") >> // fixme rm ${sv.version}
                handler.deleteContract(sv.subject)
            )
          case NOOP =>
            logger.info("NOOP record")
          case OTHER =>
            logger.error("Other record type (not a _schema topic record)")
      }
    
    def processEventRecord(key: String,
                           recordOpt: Option[String],
                           handler: ContractsHandler[IO]): IO[Unit] =
      def updateOrDeleteContract(subject: String, version: Int, isMerged: Boolean): IO[Unit] =
        val title = s"PR for $subject:$version"
        if isMerged then
          logger.info(s"$title was merged") >>
            handler.updateIsMergedStatus(subject, version)
        else
          logger.info(s"$title was rejected") >>
            handler.deleteContractVersion(subject, version)
      
      val prClosedKey = parseRaw[PrClosedKey](key.some)
      val prClosed = parseRaw[PrClosed](recordOpt)
      for
        key      <- IO.fromEither[PrClosedKey](prClosedKey)
        pr       <- IO.fromEither[PrClosed](prClosed)
        isMerged = pr.isMerged
        _        <- updateOrDeleteContract(key.subject, key.version, isMerged)
      yield ()
    
    (for
      session         <- Session.pooled[IO](host = postgres.host, port = postgres.port, user = postgres.user,
                            database = postgres.database, password = postgres.password.some, max = 10)
      repo            <- ContractsRepository.make[IO](session)
      client          <- EmberClientBuilder.default[IO].build
      contractsProducer <- ContractCreateEventsKafkaProducer.make[IO](kafka.contractsCreatedTopic, kafka.producerProps.bootstrapServers.head)
      schemasConsumer <- SchemasKafkaConsumerImpl.make[IO](NonEmptyList.one(kafka.schemasTopic), consumerSettings, contractsProducer)
      gitClient       <- GitHubClient.make[IO](contractConfig.owner, contractConfig.repo, contractConfig.path,
                                      contractConfig.baseBranch, client, Some(contractConfig.token))
      handler         <- ContractsHandler.make[IO](repo, gitClient)
    yield (schemasConsumer, handler)).use {
      case (sc, h) =>
        logger.info("Starting contracts registry") >>
          sc.process()
    }.as(ExitCode.Success)
