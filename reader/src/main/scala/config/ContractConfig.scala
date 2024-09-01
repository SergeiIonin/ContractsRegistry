package io.github.sergeiionin.contractsregistrator
package config

import pureconfig.*
import pureconfig.generic.derivation.default.*
import com.typesafe.config.ConfigFactory

// fixme give it a better name
final case class ContractConfig(
    owner: String,
    repo: String,
    token: String,
    path: String,
    baseBranch: String
) derives ConfigReader

object ContractConfig:
  def load =
    ConfigSource
      .fromConfig(ConfigFactory.load("application.conf"))
      .at("contract")
      .loadOrThrow[ContractConfig]
