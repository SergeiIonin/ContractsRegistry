package io.github.sergeiionin.contractsregistrator
package github

import cats.Monad
import cats.MonadThrow
import cats.effect.Concurrent
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.applicative.*
import cats.syntax.applicativeError.*
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}
import org.http4s.client.Client
import github4s.Github
import github4s.GHResponse
import github4s.domain.{
  Branch,
  NewPullRequestData,
  Project,
  TreeData,
  TreeDataBlob,
  TreeDataSha
}
import github4s.GithubClient
import github4s.GithubClient
import github4s.algebras.GithubAPIs
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.{
  AuthScheme,
  Credentials,
  EntityDecoder,
  EntityEncoder,
  Header,
  Headers,
  MediaType,
  Method,
  Request,
  Uri
}
import org.typelevel.ci.CIString
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.headers.{Accept, Authorization}
import org.http4s.headers.Authorization.given
import org.http4s.headers.Accept.given
import org.http4s.Credentials.Token
import org.http4s.AuthScheme.Bearer
import domain.Contract

import java.nio.file.{Files, Paths}

class GitHubClientImpl[F[_]: Concurrent: MonadThrow: Logger](
    owner: String,
    repo: String,
    path: String,
    baseBranch: String,
    client: Client[F],
    token: Option[String])
    extends GitHubClient[F]:
  private val gh = GithubClient[F](client, token)
  private val logger = summon[Logger[F]]

  given commitDataEncoder: EntityEncoder[F, CommitData] = jsonEncoderOf[F, CommitData]
  given commitDataDecoder: EntityDecoder[F, CommitData] = jsonOf[F, CommitData]

  def getFileName(subject: String, version: Int): String =
    s"${subject}_v$version.proto"

  private def getFileName(contract: Contract): String =
    getFileName(contract.subject, contract.version)

  def getBranchName(prefix: String, subject: String, version: Int): String =
    s"$prefix-$subject-$version"

  override def getLatestSHA(): F[String] =
    for
      response <- gh.repos.listCommits(owner, repo)
      latestSha <- response.result match
        case Left(err) =>
          new RuntimeException(s"error fetching list of commits: ${err.getMessage()}")
            .raiseError[F, String]
        case Right(commits) =>
          commits.headOption match
            case None =>
              new RuntimeException(s"no commits found in the $repo").raiseError[F, String]
            case Some(commit) => commit.sha.pure[F]
    yield latestSha

  override def createBranch(sha: String, branch: String): F[Unit] =
    for
      response <- gh.gitData.createReference(owner, repo, s"refs/heads/$branch", sha)
      _ <- response.result match
        case Left(err) =>
          new RuntimeException(s"error creating ref: ${err.getMessage()}").raiseError[F, Unit]
        case Right(_) => ().pure[F]
    yield ()

  override def updateBranchRef(branch: String, newCommitSha: String): F[Unit] =
    for
      response <- gh
        .gitData
        .updateReference(
          owner,
          repo,
          s"heads/$branch",
          newCommitSha,
          true
        ) // todo should force = true?
      _ <- response.result match
        case Left(err) =>
          new RuntimeException(s"error updating branch ref: ${err.getMessage()}")
            .raiseError[F, Unit]
        case Right(_) => ().pure[F]
    yield ()

  override def getContractSha(fileName: String): F[String] =
    for
      response <- gh.repos.getContents(owner, repo, s"$path/$fileName", None)
      sha <- response.result match
        case Left(err) =>
          new RuntimeException(s"error getting file: ${err.getMessage()}").raiseError[F, String]
        case Right(contentNEL) => contentNEL.head.sha.pure[F]
    yield sha

  def addContract(contract: Contract, branch: String): F[String] =
    val fileName = getFileName(contract)
    for
      response <- gh
        .repos
        .createFile(
          owner,
          repo,
          s"$path/$fileName",
          s"Add contract $fileName",
          contract.schema.getBytes,
          Some(branch))
      sha <- response.result match
        case Left(err) =>
          new RuntimeException(s"error adding file: ${err.getMessage()}").raiseError[F, String]
        case Right(response) => response.commit.sha.pure[F]
    yield sha

  override def deleteContract(
      subject: String,
      version: Int,
      sha: String,
      branch: String): F[String] =
    val fileName = getFileName(subject, version)
    for
      response <- gh
        .repos
        .deleteFile(owner, repo, s"$path/$fileName", "Delete contract", sha, Some(branch))
      sha <- response.result match
        case Left(err) =>
          new RuntimeException(s"error deleting file: ${err.getMessage()}")
            .raiseError[F, String]
        case Right(response) => response.commit.sha.pure[F]
    yield sha

  override def createPR(title: String, body: String, head: String): F[Unit] =
    for
      response <- gh
        .pullRequests
        .createPullRequest(
          owner,
          repo,
          NewPullRequestData(title, body, false),
          head,
          baseBranch)
      _ <- response.result match
        case Left(err) =>
          new RuntimeException(s"error creating PR: ${err.getMessage()}").raiseError[F, Unit]
        case Right(pr) => logger.info(s"Created PR ${pr.title}, #${pr.number}")
    yield ()
