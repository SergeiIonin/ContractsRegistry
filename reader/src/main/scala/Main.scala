package io.github.sergeiionin.contractsregistrator

import config.ApplicationConfig
import consumers.KafkaEventsConsumer.given
import consumers.contracts.ContractsKafkaConsumerImpl
import consumers.schemas.KeyType.*
import consumers.schemas.{KeyType, SchemasKafkaConsumerImpl}
import domain.events.contracts.{ContractEvent, ContractEventKey}
import domain.{Contract, SubjectAndVersion}
import github.{GitHubClient, GitHubService}
import handler.ContractsHandler
import producer.KafkaEventsProducer.given
import producer.contracts.ContractCreateKafkaEventsProducer
import repository.ContractsRepository
import service.ContractService

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.option.*
import cats.syntax.parallel.*
import fs2.kafka.{ConsumerSettings, Deserializer, KafkaConsumer}
import io.circe
import io.circe.syntax.given
import io.circe.{Decoder, parser}
import natchez.Trace.Implicits.noop
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}
import skunk.Session

object Main extends IOApp:
  
  override def run(args: List[String]): IO[ExitCode] =
    val config = ApplicationConfig.load
    val contractConfig = config.contract
    val kafka = config.kafka
    val postgres = config.postgres
    
    val topics = List(kafka.schemasTopic, kafka.prsTopic)
    val consumerProps = kafka.consumerProps.toMap()
    
    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    given Deserializer[IO, Bytes] = Deserializer.lift(bs => IO.pure(Bytes(bs)))
    
    val consumerSettings =
      ConsumerSettings
        .apply(
          Deserializer.apply[IO, Bytes],
          Deserializer.apply[IO, Bytes]
        )
        .withProperties(consumerProps)
    
    val contractsConsumerSettings =
      ConsumerSettings
        .apply(
          Deserializer.apply[IO, ContractEventKey],
          Deserializer.apply[IO, ContractEvent]
        )
        .withProperties(consumerProps)
    
    (for
      session           <- Session.pooled[IO](host = postgres.host, port = postgres.port, user = postgres.user,
                            database = postgres.database, password = postgres.password.some, max = 10)
      repo              <- ContractsRepository.make[IO](session)
      service           <- ContractService.make[IO](repo)
      client            <- EmberClientBuilder.default[IO].build
      contractsProducer <- ContractCreateKafkaEventsProducer.make[IO](kafka.contractsCreatedTopic, kafka.producerProps.bootstrapServers.head)
      schemasConsumer   <- SchemasKafkaConsumerImpl.make[IO](NonEmptyList.one(kafka.schemasTopic), consumerSettings, service, contractsProducer)
      gitClient         <- GitHubClient.make[IO](contractConfig.owner, contractConfig.repo, contractConfig.path,
        contractConfig.baseBranch, client, Some(contractConfig.token))
      gitService        <- GitHubService.make[IO](service, gitClient)
      contractsConsumer <- ContractsKafkaConsumerImpl.make[IO](
                              NonEmptyList.fromListUnsafe(List(kafka.contractsCreatedTopic, kafka.contractsDeletedTopic)),
                              contractsConsumerSettings, gitService
                           )
    yield (schemasConsumer, contractsConsumer)).use {
      case (sc, cc) =>
        logger.info("Starting contracts registry") >> {
          (sc.process(), cc.process()).parTupled
        }
    }.as(ExitCode.Success)
