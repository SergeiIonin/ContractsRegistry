package io.github.sergeiionin.contractsregistrator
package consumers

import cats.effect.Async
import fs2.kafka.{Deserializer, KafkaConsumer}
import cats.data.NonEmptyList
import domain.events.contracts.{ContractEventKey, ContractEvent, ContractCreateRequested, ContractCreateRequestedKey, ContractDeleteRequested, ContractDeleteRequestedKey}
import circe.parseArray
import io.circe.Decoder

import io.circe.Error as CirceError

abstract class KafkaEventsConsumer[F[_], K, V](kafkaConsumer: KafkaConsumer[F, K, V]) extends Consumer[F]:
  override def subscribe(topics: NonEmptyList[String]): F[Unit] =
    kafkaConsumer.subscribe(topics)

object KafkaEventsConsumer:
  private def fromEither[F[_] : Async, R : Decoder](raw: Array[Byte]): F[R] =
    Async[F].fromEither(parseArray(raw))
  
  given deserializerContractEventKey[F[_] : Async]: Deserializer[F, ContractEventKey] =
    summon[Deserializer[F, ContractEventKey]]
    
  given deserializerContractEvent[F[_] : Async]: Deserializer[F, ContractEvent] =
    summon[Deserializer[F, ContractEvent]]
  
  given deserializerContractCreateRequestedKey[F[_] : Async]: Deserializer[F, ContractCreateRequestedKey] =
    Deserializer.lift(key => fromEither(key))

  given deserializerContractCreateRequested[F[_] : Async]: Deserializer[F, ContractCreateRequested] =
    Deserializer.lift(event => fromEither(event))

  given deserializerContractDeleteRequestedKey[F[_] : Async]: Deserializer[F, ContractDeleteRequestedKey] =
    Deserializer.lift(key => fromEither(key))

  given deserializerContractDeleteRequested[F[_] : Async]: Deserializer[F, ContractDeleteRequested] =
    Deserializer.lift(event => fromEither(event))