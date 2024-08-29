package io.github.sergeiionin.contractsregistrator
package circe.codecs.domain

import domain.SubjectAndVersion

import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}
import io.circe.{Decoder, Encoder}

object SubjectAndVersionCodec:
  given encoderSubjectAndVersion: Encoder[SubjectAndVersion] = deriveEncoder[SubjectAndVersion]
  given decoderSubjectAndVersion: Decoder[SubjectAndVersion] = deriveDecoder[SubjectAndVersion]
