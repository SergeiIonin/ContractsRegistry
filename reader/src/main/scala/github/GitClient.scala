package io.github.sergeiionin.contractsregistrator
package github

import cats.effect.{Async, Resource}
import org.http4s.client.Client
import org.typelevel.log4cats.Logger

// todo path where contracts will reside should be configurable
trait GitClient[F[_]]:
  def getLatestSHA(): F[String]
  def createRef(sha: String, ref: String): F[Unit]
  def createBlob(content: String): F[String]
  def getBaseTreeSha(ref: String): F[String]
  def createNewTree(fileName: String, baseTreeSha: String, blobSha: String): F[String]
  def createCommit(newTreeSha: String, parentCommitSha: String, message: String): F[String]
  def updateBranchRef(branch: String, newCommitSha: String): F[Unit]
  def createPR(title: String, body: String, head: String): F[Unit]

object GitClient:
  def make[F[_]: Async: Logger](owner: String, repo: String, path: String,
                                baseBranch: String, client: Client[F], token: Option[String]) =
    Resource.pure[F, GitClient[F]](new GitClientImpl(owner, repo, path, baseBranch, client, token))
