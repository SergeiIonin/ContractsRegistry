package io.github.sergeiionin.contractsregistrator
package github

import domain.Contract

import cats.syntax.*
import cats.syntax.applicativeError.*
import cats.{Monad, MonadThrow}

import scala.collection.mutable
import scala.util.Random

final class GitHubClientTestImpl[F[_] : Monad : MonadThrow] extends GitHubClient[F]:
    private val generateSha: String = Random.alphanumeric.take(40).mkString

    private val shaToContract = mutable.Map[String, String]() // sha -> fileContent
    private val contractToSha = mutable.Map[String, String]() // fileName -> sha
    private val branches = mutable.Map[String, String]() // branchName -> sha

    private val latestSha = generateSha

    def getFileName(subject: String, version: Int): String = s"${subject}_v$version.proto"
    
    def getBranchName(prefix: String, subject: String, version: Int): String = s"$prefix-$subject-$version"
    
    def getLatestSHA(): F[String] = Monad[F].pure(latestSha)
    
    def createBranch(sha: String, branch: String): F[Unit] =
      Monad[F].pure {
        branches(branch) = sha
      }
    
    def getContractSha(fileName: String): F[String] =
      contractToSha.get(fileName) match
        case Some(sha) => Monad[F].pure(sha)
        case None => new RuntimeException(s"file $fileName not found").raiseError[F, String]
    
    def addContract(contract: Contract, branch: String): F[String] = 
      val fileName = getFileName(contract.subject, contract.version)
      Monad[F].pure {
        val sha = generateSha
        shaToContract(sha) = contract.schema
        contractToSha(fileName) = sha
        sha
      }
    
    def deleteContract(subject: String, version: Int, sha: String, branch: String): F[String] =
      val fileName = getFileName(subject, version)
      Monad[F].pure {
        shaToContract.remove(sha)
        contractToSha.remove(fileName)
        generateSha
      }
    
    def updateBranchRef(branch: String, newCommitSha: String): F[Unit] =
      Monad[F].pure {
        branches(branch) = newCommitSha
      }
    
    def createPR(title: String, body: String, head: String): F[Unit] = 
      Monad[F].pure(())
