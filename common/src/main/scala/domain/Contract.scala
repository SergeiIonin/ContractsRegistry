package io.github.sergeiionin.contractsregistrator
package domain

final case class Contract(
                         subject: String,
                         version: Int,
                         id: Int,
                         schema: String,
                         schemaType: SchemaType,
                         isMerged: Boolean = false, // fixme rm it
                         deleted: Option[Boolean] = None // fixme rm it
)
