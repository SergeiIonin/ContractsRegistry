package io.github.sergeiionin.contractsregistrator
package github

import cats.effect.{Async, Resource}
import domain.Contract
import org.http4s.client.Client
import org.typelevel.log4cats.Logger

// todo how to also support contracts of different types?
trait GitHubClient[F[_]]:
  def getFileName(subject: String, version: Int): String
  def getBranchName(prefix: String, subject: String, version: Int): String
  def getLatestSHA(): F[String]
  def createBranch(sha: String, branch: String): F[Unit]
  def getContractSha(fileName: String): F[String]
  def addContract(contract: Contract, branch: String): F[String]
  def deleteContract(subject: String, version: Int, sha: String, branch: String): F[String]
  def updateBranchRef(branch: String, newCommitSha: String): F[Unit]
  def createPR(title: String, body: String, head: String): F[Unit]

object GitHubClient:
  def make[F[_]: Async: Logger](owner: String, repo: String, path: String,
                                baseBranch: String, client: Client[F], token: Option[String]) =
    Resource.pure[F, GitHubClient[F]](new GitHubClientImpl(owner, repo, path, baseBranch, client, token))
  
  def test[F[_] : Async : Logger]() =
    Resource.pure[F, GitHubClient[F]](GitHubClientTestImpl[F])