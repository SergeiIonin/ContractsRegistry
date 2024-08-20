package io.github.sergeiionin.contractsregistrator
package domain

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import domain.SchemaType

import domain.SchemaType.PROTOBUF

class ContractPullRequestSpec extends AnyWordSpec with Matchers {

  "ContractPullRequest" should {

    "return the correct subject and version in getBody" in {
      val pr = ContractPullRequest("testSubject", 1, isDeleted = false)
      pr.getBody() shouldEqual "testSubject_1"
    }

    "return the correct title for added contract in getTitle" in {
      val pr = ContractPullRequest("testSubject", 1, isDeleted = false)
      pr.getTitle() shouldEqual "Add contract testSubject_1"
    }
    
    "return the correct title for deleted contract in getTitle" in {
      val pr = ContractPullRequest("testSubject", 1, isDeleted = true)
      pr.getTitle() shouldEqual "Delete contract testSubject_1"
    }

    "create a ContractPullRequest from a Contract in fromContract" in {
      val contract = Contract("testSubject", 1, 123, "schema", PROTOBUF, false, None)
      val pr = ContractPullRequest.fromContract(contract)
      pr shouldEqual ContractPullRequest("testSubject", 1, isDeleted = false)
    }

    "create a ContractPullRequest from a valid raw string in fromRaw" in {
      val raw = "testSubject_1"
      val pr = ContractPullRequest.fromRaw(raw, isDeleted = false)
      pr shouldEqual Right(ContractPullRequest("testSubject", 1, isDeleted = false))
    }

    "return an error for an invalid raw string in fromRaw" in {
      val raw = "testSubject:1"
      val pr = ContractPullRequest.fromRaw(raw, isDeleted = false)
      pr shouldEqual Left(s"Invalid contract pull request format: $raw, the format should be <subject>_<version>")
    }

    "return an error for a non-integer version in fromRaw" in {
      val raw = "testSubject_notAnInt"
      val pr = ContractPullRequest.fromRaw(raw, isDeleted = false)
      pr shouldEqual Left("version notAnInt is not a valid integer")
    }
  }
}
