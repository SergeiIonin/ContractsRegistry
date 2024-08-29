package io.github.sergeiionin.contractsregistrator
package consumers

import circe.codecs.domain.events.contracts.ContractEventCodec.given
import circe.codecs.domain.events.contracts.ContractEventKeyCodec.given
import circe.parseArray
import domain.events.contracts.*

import cats.data.NonEmptyList
import cats.effect.Async
import fs2.kafka.{Deserializer, KafkaConsumer}
import io.circe.{Decoder, Error as CirceError}

abstract class KafkaEventsConsumer[F[_], K, V](kafkaConsumer: KafkaConsumer[F, K, V]) extends Consumer[F]:
  override def subscribe(topics: NonEmptyList[String]): F[Unit] =
    kafkaConsumer.subscribe(topics)

object KafkaEventsConsumer:
  private def fromEither[F[_] : Async, R : Decoder](raw: Array[Byte]): F[R] =
    val res = parseArray(raw)
    res match 
      case Left(err) => println(s"error parsing msg: ${err.getMessage}")
      case Right(_) => ()
    Async[F].fromEither(res)
  
  given deserializerContractEventKey[F[_] : Async]: Deserializer[F, ContractEventKey] =
    Deserializer.lift(key => fromEither(key))
    
  given deserializerContractEvent[F[_] : Async]: Deserializer[F, ContractEvent] =
    Deserializer.lift(event => fromEither(event))
  
  given deserializerContractCreateRequestedKey[F[_] : Async]: Deserializer[F, ContractCreateRequestedKey] =
    Deserializer.lift(key => fromEither(key))

  given deserializerContractCreateRequested[F[_] : Async]: Deserializer[F, ContractCreateRequested] =
    Deserializer.lift(event => fromEither(event))

  given deserializerContractDeleteRequestedKey[F[_] : Async]: Deserializer[F, ContractDeleteRequestedKey] =
    Deserializer.lift(key => fromEither(key))

  given deserializerContractDeleteRequested[F[_] : Async]: Deserializer[F, ContractDeleteRequested] =
    Deserializer.lift(event => fromEither(event))