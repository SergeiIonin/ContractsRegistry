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
import github4s.domain.{NewPullRequestData, Project, TreeData, TreeDataBlob, TreeDataSha}
import github4s.GithubClient
import github4s.GithubClient
import github4s.algebras.GithubAPIs
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.{AuthScheme, Credentials, EntityDecoder, EntityEncoder, Header, Headers, MediaType, Method, Request, Uri}
import org.typelevel.ci.CIString
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.headers.{Accept, Authorization}
import org.http4s.headers.Authorization.given
import org.http4s.headers.Accept.given
import org.http4s.Credentials.Token
import org.http4s.AuthScheme.Bearer

import java.nio.file.{Files, Paths}

class GitClientImpl[F[_] : Concurrent : MonadThrow : Logger](owner: String, repo: String, mainBranch: String, client: Client[F], token: Option[String]) extends GitClient[F]:
  private val gh = GithubClient[F](client, token)
  private val logger = summon[Logger[F]]

  private val acceptHeader = Header.Raw(CIString("Accept"), "application/vnd.github.v3+json")
  private val gitHubApiVersionHeader = Header.Raw(CIString("X-GitHub-Api-Version"), "2022-11-28")
  private val authHeader = Authorization(Token(Bearer, token.getOrElse("")))
  private val contentType = Accept(MediaType.application.json)
  
  given commitDataEncoder: EntityEncoder[F, CommitData] = jsonEncoderOf[F, CommitData]
  given commitDataDecoder: EntityDecoder[F, CommitData] = jsonOf[F, CommitData]

  private def postRequest[T](uri: Uri, entity: T)(using EntityEncoder[F, T]): Request[F] =
    Request[F](
      Method.POST, uri,
      headers = Headers(authHeader, acceptHeader, gitHubApiVersionHeader, contentType))
      .withEntity(entity)
    
  private def getRequest(uri: Uri): Request[F] =
    Request[F](
      Method.GET, uri,
      headers = Headers(authHeader, acceptHeader, gitHubApiVersionHeader, contentType)
    )

  override def getLatestSHA(): F[String] =
    for
      response <- gh.repos.listCommits(owner, repo)
      latestSha <- response.result match
        case Left(err) => new RuntimeException(s"error fetching list of commits: ${err.getMessage()}").raiseError[F, String] //MonadThrow.raiseError[F, String]()
        case Right(commits) => commits.lastOption match
          case None => new RuntimeException(s"no commits found in the $repo").raiseError[F, String]
          case Some(commit) => commit.sha.pure[F]
    yield latestSha

  override def createRef(sha: String, ref: String): F[Unit] =
    for
      response <- gh.gitData.createReference(owner, repo, s"refs/heads/$ref", sha)
      _ <- response.result match
        case Left(err) => new RuntimeException(s"error creating ref: ${err.getMessage()}").raiseError[F, Unit]
        case Right(_) => ().pure[F]
    yield ()

  override def createBlob(content: String): F[String] =
    for
      response <- gh.gitData.createBlob(owner, repo, content, Some("utf-8"))
      blobSha <- response.result match
        case Left(err) => new RuntimeException(s"error creating blob: ${err.getMessage()}").raiseError[F, String]
        case Right(blob) => logger.info(s"created blob.sha = ${blob.sha}") >> blob.sha.pure[F]
    yield blobSha

  override def getBaseTreeSha(sha: String): F[String] =
    val req = getRequest(Uri.unsafeFromString(s"https://api.github.com/repos/$owner/$repo/commits/$sha"))
    client.run(req).use(resp =>
      logger.info(s"Fetching base tree sha") >> {
        resp.status match
          case org.http4s.Status.Ok =>
            resp.as[CommitData].map(_.commit.tree.sha)
              .flatTap(sha => logger.info(s"sha on master: $sha}"))
          case _ => new RuntimeException(s"Failed to fetch base tree sha: ${resp.status}").raiseError[F, String]
      }
    )

  override def createNewTree(path: String, baseTreeSha: String, blobSha: String): F[String] =
    for
      response <- gh.gitData.createTree(owner, repo, Some(baseTreeSha), List(TreeDataSha(path, "100644", "blob", blobSha)))
      newTreeSha <- response.result match
        case Left(err) => new RuntimeException(s"error creating tree: ${err.getMessage()}").raiseError[F, String]
        case Right(tree) => tree.sha.pure[F]
    yield newTreeSha

  override def createCommit(newTreeSha: String, parentCommitSha: String, message: String): F[String] = 
    for
      response     <- gh.gitData.createCommit(owner, repo, message, newTreeSha, List(parentCommitSha), None)
      newCommitSha <- response.result match
                        case Left(err) => new RuntimeException(s"error creating commit: ${err.getMessage()}").raiseError[F, String]
                        case Right(commit) => commit.sha.pure[F]
    yield newCommitSha

  override def updateBranchRef(branch: String, newCommitSha: String): F[Unit] = 
    for
      response <- gh.gitData.updateReference(owner, repo, s"heads/$branch", newCommitSha, true) // todo should force = true?
      _        <- response.result match
                    case Left(err) => new RuntimeException(s"error updating branch ref: ${err.getMessage()}").raiseError[F, Unit]
                    case Right(_) => ().pure[F]
    yield ()

  override def createPR(title: String, body: String, head: String, base: String): F[Unit] = 
    for
      response <- gh.pullRequests.createPullRequest(owner, repo, NewPullRequestData(title, body, false), head, base)
      _ <- response.result match
        case Left(err) => new RuntimeException(s"error creating PR: ${err.getMessage()}").raiseError[F, Unit]
        case Right(pr) => logger.info(s"Created PR ${pr.title}, #${pr.number}")
    yield ()
