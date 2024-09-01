package io.github.sergeiionin.contractsregistrator
package config

import pureconfig.*
import pureconfig.generic.derivation.default.*

final case class RestApiConfig(
    host: String,
    port: Int
) derives ConfigReader
