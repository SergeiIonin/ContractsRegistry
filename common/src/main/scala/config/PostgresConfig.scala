package io.github.sergeiionin.contractsregistrator
package config

import com.typesafe.config.ConfigFactory
import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class PostgresConfig(
                            host: String,
                            port: Int,
                            user: String,
                            password: String,
                            database: String,
                         ) derives ConfigReader
