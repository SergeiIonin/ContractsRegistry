package io.github.sergeiionin.contractsregistrator

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO, IOApp}
import fs2.kafka.{ConsumerSettings, Deserializer, KafkaConsumer}
import handler.ContractsHandler
import repository.ContractsRepository
import io.circe.parser
import io.circe
import domain.Contract
import github.GitClient
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}
import org.http4s.ember.client.EmberClientBuilder
import skunk.Session
import natchez.Trace.Implicits.noop
import io.github.sergeiionin.contractsregistrator.config.ApplicationConfig

object Main extends IOApp:
  opaque type Bytes = Array[Byte]
  
  override def run(args: List[String]): IO[ExitCode] =
    val config = ApplicationConfig.load
    val contractConfig = config.contract
    val kafka = config.kafka
    val postgres = config.postgres
    
    val topics = List(kafka.schemasTopic)
    val consumerProps = kafka.consumerProps.toMap()
    
    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    def toContract(recordRaw: String): Either[circe.Error, Contract] =
      parser.parse(recordRaw).flatMap(json => {
        json.as[Contract]
      })
    
    val consumerSettings =
      ConsumerSettings
        .apply(
          Deserializer.apply[IO, Bytes],
          Deserializer.apply[IO, Bytes])
        .withProperties(consumerProps)
    
    (for
      session   <- Session.single[IO](host = postgres.host, port = postgres.port, user = postgres.user, database = postgres.database)
      repo      <- ContractsRepository.make[IO](session)
      client    <- EmberClientBuilder.default[IO].build
      consumer  <- KafkaConsumer.resource[IO, Bytes, Bytes](consumerSettings)
      gitClient <- GitClient.make[IO](contractConfig.owner, contractConfig.repo, contractConfig.path,
                                      contractConfig.baseBranch, client, Some(contractConfig.token))
      handler   <- ContractsHandler.make[IO](repo, gitClient)
    yield (consumer, handler)).use {
      case (c, h) =>
        logger.info("Starting contracts registrator") >>
        c.subscribe(NonEmptyList.fromListUnsafe(topics)) >>
          logger.info(s"Subscribed to topics ${topics.mkString(", ")}") >>
          c.stream
            .evalMap(cr =>
              val recordOpt = Option(cr.record.value).map(bytes => new String(bytes))
              logger.info(s"Received record: ${recordOpt.getOrElse("N/A")}") >> {
                recordOpt match
                  case None => IO.unit
                  case Some(record) =>
                    val contract = toContract(record)
                    contract.fold[IO[Unit]](
                      e => logger.error(s"Failed to parse contract: ${e.getMessage}"),
                      c => logger.info(s"Registering new contract: $c") >> h.handle(c)
                    )
              }
            )
            .compile
            .drain
    }.as(ExitCode.Success)
