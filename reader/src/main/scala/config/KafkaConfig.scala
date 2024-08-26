package io.github.sergeiionin.contractsregistrator
package config

import com.typesafe.config.ConfigFactory
import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class KafkaConfig(
                           schemasTopic: String,
                           prsTopic: String,
                           contractsCreatedTopic: String,
                           contractsDeletedTopic: String,
                           producerProps: ProducerProps,
                           consumerProps: ConsumerProps
                         ) derives ConfigReader

final case class ProducerProps(
  bootstrapServers: List[String]
) derives ConfigReader

final case class ConsumerProps(
  bootstrapServers: List[String],
  groupId: String,
  autoOffsetReset: String
) derives ConfigReader:
  def toMap() = 
    Map(
      "bootstrap.servers" -> bootstrapServers.mkString(","),
      "group.id" -> groupId,
      "auto.offset.reset" -> autoOffsetReset
    )

object KafkaConfig:
  def load =
    ConfigSource.fromConfig(ConfigFactory.load("application.conf")).at("kafka").loadOrThrow[KafkaConfig]
