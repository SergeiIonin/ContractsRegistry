package io.github.sergeiionin.contractsregistrator
package config

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class ConfigSpecs extends AnyWordSpec with Matchers:
  "ApplicationConfig" should {
    "load" in {
      val config = ApplicationConfig.load
      val contract = config.contract
      contract.owner shouldEqual "your_owner"
      contract.repo shouldEqual "your_repo"
      contract.token shouldEqual "your_token"
      contract.path shouldEqual "your_path"
      contract.baseBranch shouldEqual "your_base_branch"
      
      val kafka = config.kafka
      kafka.schemasTopic shouldEqual "_schemas"
      val consumerProps = kafka.consumerProps
      consumerProps.bootstrapServers shouldEqual List("localhost:19092")
      consumerProps.groupId shouldEqual "contracts-registrator-reader"
      consumerProps.autoOffsetReset shouldEqual "latest"
      
      val postgres = config.postgres
      postgres.host shouldEqual "localhost"
      postgres.port shouldEqual 5434
      postgres.user shouldEqual "postgres"
      postgres.password shouldEqual "postgres"
      postgres.database shouldEqual "contracts"
    }
  }
