package io.github.sergeiionin.contractsregistrator
package producer.events.prs

import producer.events.{Key, Event}
import io.circe.{Encoder, Decoder}

final case class PrClosed(subject: String, version: Int, isMerged: Boolean) extends Event derives Encoder, Decoder
final case class PrClosedKey(subject: String, version: Int) extends Key derives Encoder, Decoder
