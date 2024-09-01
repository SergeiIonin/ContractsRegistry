package io.github.sergeiionin.contractsregistrator

import config.ApplicationConfig
import consumers.KafkaEventsConsumer.given
import consumers.contracts.ContractsKafkaConsumerImpl
import domain.events.contracts.{ContractEvent, ContractEventKey}
import domain.{Contract, SubjectAndVersion}
import github.{GitHubClient, GitHubService}

import cats.data.NonEmptyList
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.option.*
import fs2.kafka.{ConsumerSettings, Deserializer}
import org.http4s.ember.client.EmberClientBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}

object Main extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =
    val config = ApplicationConfig.load
    val contractConfig = config.contract
    val kafka = config.kafka

    val consumerProps = kafka.consumerProps.toMap()

    given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

    val contractsConsumerSettings =
      ConsumerSettings
        .apply(
          Deserializer.apply[IO, ContractEventKey],
          Deserializer.apply[IO, ContractEvent]
        )
        .withProperties(consumerProps)

    (for
      client <- EmberClientBuilder.default[IO].build
      gitClient <- GitHubClient.make[IO](
        contractConfig.owner,
        contractConfig.repo,
        contractConfig.path,
        contractConfig.baseBranch,
        client,
        Some(contractConfig.token))
      gitService <- GitHubService.make[IO](gitClient)
      contractsConsumer <- ContractsKafkaConsumerImpl.make[IO](
        NonEmptyList.fromListUnsafe(
          List(kafka.contractsCreatedTopic, kafka.contractsDeletedTopic)),
        contractsConsumerSettings,
        gitService
      )
    yield contractsConsumer)
      .use { consumer =>
        logger.info("Starting contracts registry") >>
          consumer.process()
      }
      .as(ExitCode.Success)
