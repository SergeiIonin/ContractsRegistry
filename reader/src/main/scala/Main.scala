package io.github.sergeiionin.contractsregistrator

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO, IOApp}
import fs2.kafka.{ConsumerSettings, Deserializer, KafkaConsumer}
import handler.ContractsHandler
import repository.ContractsRepository
import handler.protos.ProtosHandler
import io.circe.parser
import io.circe
import domain.Contract
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}
import skunk.Session
import natchez.Trace.Implicits.noop
import os.*

object Main extends IOApp:
  opaque type Bytes = Array[Byte]
  
  override def run(args: List[String]): IO[ExitCode] =
    val topics = List("_schemas")
    val props = Map(
      "bootstrap.servers" -> "localhost:19092", // fixme
      "group.id" -> "contracts-registrator-reader",
      "auto.offset.reset" -> "earliest",
    )
    val dbHost = "localhost" // fixme
    val dbPort = 5434
    val user = "postgres"
    val database = "foo" //"contracts"
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
        .withProperties(props)
    
    (for
      session  <- Session.single[IO](host = dbHost, port = dbPort, user = user, database = database)
      repo     <- ContractsRepository.make[IO](session)
      consumer <- KafkaConsumer.resource[IO, Bytes, Bytes](consumerSettings)
      protos   <- ProtosHandler.make[IO]("proto/src/main/protobuf/io/github/sergeiionin/contractsregistrator/proto")
      handler  <- ContractsHandler.make[IO](repo, protos)
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
