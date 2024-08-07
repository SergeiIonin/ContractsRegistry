package io.github.sergeiionin.contractsregistrator
package github

import io.circe.{Encoder, Decoder}

final case class Tree(sha: String, url: String) derives Encoder, Decoder

final case class Commit(tree: Tree) derives Encoder, Decoder

final case class CommitData(sha: String, commit: Commit) derives Encoder, Decoder
