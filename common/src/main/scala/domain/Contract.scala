package io.github.sergeiionin.contractsregistrator
package domain

import io.circe.{Decoder, Encoder}

final case class Contract(
                         subject: String,
                         version: Int,
                         id: Int,
                         schema: String,
                         schemaType: SchemaType,
                         isMerged: Boolean = false,
                         deleted: Option[Boolean] = None
) derives Encoder, Decoder

final case class SubjectAndVersion(subject: String, version: Int) derives Encoder, Decoder
