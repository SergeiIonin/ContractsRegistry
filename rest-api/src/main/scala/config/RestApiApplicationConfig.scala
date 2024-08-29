package io.github.sergeiionin.contractsregistrator
package config

import com.typesafe.config.ConfigFactory
import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class RestApiApplicationConfig(
                                    restApi: RestApiConfig,
                                    schemaRegistry: SchemaRegistryConfig,
                                    postgres: PostgresConfig,
                                    kafkaProducer: KafkaProducerConfig,
                                  ) derives ConfigReader

object RestApiApplicationConfig:
    def load: RestApiApplicationConfig =
      ConfigSource.fromConfig(ConfigFactory.load("application.conf")).loadOrThrow[RestApiApplicationConfig]
