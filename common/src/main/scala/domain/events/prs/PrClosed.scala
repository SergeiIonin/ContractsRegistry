package io.github.sergeiionin.contractsregistrator
package domain
package events
package prs

import io.circe.{Decoder, Encoder}

// todo add title
final case class PrClosed(subject: String, version: Int, isMerged: Boolean) extends Event derives Encoder, Decoder
final case class PrClosedKey(subject: String, version: Int) extends Key derives Encoder, Decoder
