package io.github.sergeiionin.contractsregistrator
package github

import github4s.Github
import github4s.GHResponse
import github4s.domain.NewPullRequestData
import github4s.GithubClient
import org.http4s.client.Client
import cats.effect.IO


class GitClientImpl[F[_]](client: Client[F], token: Option[String]) extends GitClient[F]:
  def createPullRequest(owner: String, repo: String, newPR: NewPullRequestData): F[Unit] = {
    val gh = GithubClient[F](client, token)
    gh.repos.createFile()
  }