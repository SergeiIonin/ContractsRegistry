package io.github.sergeiionin.contractsregistrator

import cats.effect.{ExitCode, IO, IOApp}
import fs2.kafka.{ConsumerSettings, Deserializer, KafkaConsumer}
import handler.ContractsHandler
import repository.ContractsRepository

import io.circe.parser
import domain.Contract

import io.circe
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory, SelfAwareStructuredLogger}
import skunk.Session

object Main extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    val topics = List("_schemas")
    val props = Map(
      "bootstrap.servers" -> "localhost:9092", // fixme
      "group.id" -> "contracts-registrator-reader",
      "auto.offset.reset" -> "earliest",
    )
    val dbHost = "localhost" // fixme
    val user = "postgres"
    val database = "contracts"
    given val logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    val consumerSettings = ConsumerSettings.apply(Deserializer.apply[IO, String], Deserializer.apply[IO, String]).withProperties(props)
    (for
      session  <- Session.single[IO](host = dbHost, user = user, database = database)
      repo     <- ContractsRepository.make[IO](session)
      consumer <- KafkaConsumer.resource[IO, String, String](consumerSettings)
      handler  <- ContractsHandler.make[IO](repo)
    yield (consumer, handler)).use {
      case (c, h) =>
        c.subscribe(topics) >>
          c.stream
            .evalMap(cr => {
              val contract = toContract(cr.record.value)
              contract.fold(
                e => logger.error(s"Failed to parse contract: ${e.getMessage}"),
                c => logger.info(s"Registering new contract: $c") >> h.handle(c)
              )
            })
            .compile
            .drain
    }.as(ExitCode.Success)
    
    def toContract(recordRaw: String): Either[circe.Error, Contract] =
      parser.parse(recordRaw).flatMap(json => {
        json.as[Contract]
      })
