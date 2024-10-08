package io.github.sergeiionin.contractsregistrator
package consumers
package contracts

import cats.syntax.flatMap.*
import consumers.Consumer
import consumers.KafkaEventsConsumer
import domain.events.contracts.{
  ContractCreateRequestedKey,
  ContractCreateRequested,
  ContractVersionDeleteRequestedKey,
  ContractVersionDeleteRequested,
  ContractDeleteRequestedKey,
  ContractDeleteRequested,
  ContractEventKey,
  ContractEvent
}
import github.{GitHubClient, GitHubService}
import cats.effect.{Async, Resource}
import cats.data.NonEmptyList
import fs2.kafka.{CommittableConsumerRecord, KafkaConsumer, ConsumerSettings}
import org.typelevel.log4cats.Logger

class ContractsKafkaConsumerImpl[F[_]: Async: Logger](
    topics: NonEmptyList[String],
    gitHubService: GitHubService[F],
    kafkaConsumer: KafkaConsumer[F, ContractEventKey, ContractEvent]
) extends KafkaEventsConsumer[F, ContractEventKey, ContractEvent](kafkaConsumer):
  private val logger = summon[Logger[F]]

  override def process(): F[Unit] =
    subscribe(topics) >>
      kafkaConsumer
        .stream
        .evalMap { cr =>
          logger.info(s"Processing contract event: ${cr}") >> {
            cr.record.value match
              case contractCreated: ContractCreateRequested =>
                gitHubService.addContract(contractCreated.contract)
              case contractVersionDeleted: ContractVersionDeleteRequested =>
                gitHubService.deleteContractVersion(
                  contractVersionDeleted.subject,
                  contractVersionDeleted.version)
              case contractDeleted: ContractDeleteRequested =>
                gitHubService.deleteContract(contractDeleted.subject, contractDeleted.versions)
          }
        }
        .compile
        .drain

object ContractsKafkaConsumerImpl:
  def make[F[_]: Async: Logger](
      topics: NonEmptyList[String],
      consumerSettings: ConsumerSettings[F, ContractEventKey, ContractEvent],
      gitHubService: GitHubService[F]
  ): Resource[F, Consumer[F]] =
    KafkaConsumer
      .resource[F, ContractEventKey, ContractEvent](consumerSettings)
      .map(consumer => ContractsKafkaConsumerImpl[F](topics, gitHubService, consumer))
