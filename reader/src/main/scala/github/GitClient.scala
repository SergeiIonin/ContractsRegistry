package io.github.sergeiionin.contractsregistrator
package github

trait GitClient[F[_]]:
  def createPullRequest(contractName: String): F[Unit]

