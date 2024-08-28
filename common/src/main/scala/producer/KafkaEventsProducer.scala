package io.github.sergeiionin.contractsregistrator
package producer

import circe.codecs.domain.events.contracts.ContractEventCodec.given
import circe.codecs.domain.events.contracts.ContractEventKeyCodec.given
import domain.events.contracts.*

import cats.effect.Async
import cats.syntax.applicative.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerSettings, Serializer}
import io.circe.syntax.*

abstract class KafkaEventsProducer[F[_] : Async, K, V]() extends EventsProducer[F, K, V]:
  def kafkaProducer: KafkaProducer[F, K, V]
  
  override def produce(key: K, value: V): F[Unit] =
    kafkaProducer.produceOne(ProducerRecord(this.topic, key, value)).flatten.void

object KafkaEventsProducer:  
  def producerSettings[F[_], K, V](bootstrapServers: String)(using Serializer[F, K], Serializer[F, V]): ProducerSettings[F, K, V] =
    ProducerSettings[F, K, V](
      Serializer.apply[F, K],
      Serializer.apply[F, V],
    ).withBootstrapServers(bootstrapServers)
  
  given serializerContractEventKey[F[_] : Async]: Serializer[F, ContractEventKey] =
    Serializer.lift(key => key.asJson.noSpaces.getBytes.pure[F])
    
  given serializerContractEvent[F[_] : Async]: Serializer[F, ContractEvent] =
    Serializer.lift(key => key.asJson.noSpaces.getBytes.pure[F])

  given serializerContractCreateRequestedKey[F[_] : Async]: Serializer[F, ContractCreateRequestedKey] =
    Serializer.lift(key => key.asJson.noSpaces.getBytes.pure[F])

  given serializerContractCreateRequested[F[_] : Async]: Serializer[F, ContractCreateRequested] =
    Serializer.lift(event => event.asJson.noSpaces.getBytes.pure[F])

  given serializerContractDeleteRequestedKey[F[_] : Async]: Serializer[F, ContractDeletedEventKey] =
    Serializer.lift(key => key.asJson.noSpaces.getBytes.pure[F])

  given serializerContractDeleteRequested[F[_] : Async]: Serializer[F, ContractDeletedEvent] =
    Serializer.lift(event => event.asJson.noSpaces.getBytes.pure[F])
