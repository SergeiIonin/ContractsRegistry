package io.github.sergeiionin.contractsregistrator
package client.schemaregistry

import client.SchemaClient

trait SchemaRegistryPaths[F[_]]:
  schemasClient: SchemaClient[F] =>
  
  val subjects = "subjects"
  val versions = "versions"
