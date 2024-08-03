package io.github.sergeiionin.contractsregistrator
package github

import cats.Monad
import cats.MonadThrow
import cats.syntax.flatMap.*
import cats.syntax.applicativeError.*
import github4s.Github
import github4s.GHResponse
import github4s.domain.NewPullRequestData
import github4s.GithubClient
import org.http4s.client.Client
import cats.effect.IO

import java.nio.file.{Files, Paths}

class GitClientImpl[F[_] : Monad : MonadThrow : Logger](owner: String, repo: String, client: Client[F], token: Option[String]) extends GitClient[F]:
  def createPullRequest(contractName: String): F[Unit] = {
    val gh = GithubClient[F](client, token)
    val filePath = "proto/src/main/protobuf/io/github/sergeiionin/contractsregistrator/proto/$contractName.proto"
    gh.repos.createFile(owner, repo,
        s"proto/src/main/protobuf/io/github/sergeiionin/contractsregistrator/proto/$contractName.proto",
        s"add new contract $contractName",
        Files.readAllBytes(Paths.get(filePath)),
        Some(s"add-contract-$contractName.proto"),
        )
      .flatMap(ghr => ghr.statusCode)
      .handleErrorWith(t => MonadThrow[F].raiseError(new RuntimeException(s"Failed to create pull request for contract $contractName: ${t.getMessage}")))
      .map(code => {
        if (code >= 200 && code < 300) ()
        else {
          
        }
      })
  }