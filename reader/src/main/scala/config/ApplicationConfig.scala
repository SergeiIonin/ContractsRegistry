package io.github.sergeiionin.contractsregistrator
package config

import pureconfig.*
import pureconfig.generic.derivation.default.*
import com.typesafe.config.ConfigFactory

final case class ApplicationConfig(
    kafka: KafkaConfig,
    contract: ContractConfig
) derives ConfigReader

object ApplicationConfig:
  def load: ApplicationConfig =
    ConfigSource
      .fromConfig(ConfigFactory.load("application.conf"))
      .loadOrThrow[ApplicationConfig]
