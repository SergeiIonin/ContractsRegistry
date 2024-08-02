package io.github.sergeiionin.contractsregistrator
package github

trait GitClient[F[_]]:
  def createPullRequest(owner: String, repo: String, newPR: NewPullRequestData): F[Unit]

