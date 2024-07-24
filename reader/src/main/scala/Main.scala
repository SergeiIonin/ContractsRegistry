package io.github.sergeiionin.contractsregistrator

import cats.data.NonEmptyList
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
import natchez.Trace.Implicits.noop

object Main extends IOApp:
  override def run(args: List[String]): IO[ExitCode] =
    val topics = List("_schemas")
    val props = Map(
      "bootstrap.servers" -> "localhost:19092", // fixme
      "group.id" -> "contracts-registrator-reader",
      "auto.offset.reset" -> "earliest",
    )
    val dbHost = "http://localhost:5434" // fixme
    val user = "postgres"
    val database = "contracts"
    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    def toContract(recordRaw: String): Either[circe.Error, Contract] =
      parser.parse(recordRaw).flatMap(json => {
        json.as[Contract]
      })

    val consumerSettings = ConsumerSettings.apply(Deserializer.apply[IO, String], Deserializer.apply[IO, String]).withProperties(props)
    
    (for
      session  <- Session.single[IO](host = dbHost, user = user, database = database)
      repo     <- ContractsRepository.make[IO](session)
      consumer <- KafkaConsumer.resource[IO, String, String](consumerSettings)
      handler  <- ContractsHandler.make[IO](repo)
    yield (consumer, handler)).use {
      case (c, h) =>
        c.subscribe(NonEmptyList.fromListUnsafe(topics)) >>
          c.stream
            .evalMap(cr => {
              val contract = toContract(cr.record.value)
              contract.fold[IO[Unit]](
                e => logger.error(s"Failed to parse contract: ${e.getMessage}"),
                c => logger.info(s"Registering new contract: $c") >> h.handle(c)
              )
            })
            .compile
            .drain
    }.as(ExitCode.Success)
