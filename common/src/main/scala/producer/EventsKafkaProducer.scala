package io.github.sergeiionin.contractsregistrator
package producer

import domain.events.contracts.{ContractCreateRequestedKey, ContractCreateRequested,
  ContractDeleteRequestedKey, ContractDeleteRequested}
import cats.effect.Async
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.applicative.*
import fs2.kafka.{KafkaProducer, ProducerRecord, ProducerSettings, Serializer}
import io.circe.syntax.*

abstract class EventsKafkaProducer[F[_] : Async, K, V]() extends EventsProducer[F, K, V]:
  def kafkaProducer: KafkaProducer[F, K, V]
  
  override def produce(key: K, value: V): F[Unit] =
    kafkaProducer.produceOne(ProducerRecord(this.topic, key, value)).flatten.void

object EventsKafkaProducer:  
  def producerSettings[F[_], K, V](bootstrapServers: String)(using Serializer[F, K], Serializer[F, V]): ProducerSettings[F, K, V] =
    ProducerSettings[F, K, V](
      Serializer.apply[F, K],
      Serializer.apply[F, V],
    ).withBootstrapServers(bootstrapServers)
  
  given serializerContractCreateRequestedKey[F[_] : Async]: Serializer[F, ContractCreateRequestedKey] =
    Serializer.lift(key => key.asJson.noSpaces.getBytes.pure[F])

  given serializerContractCreateRequested[F[_] : Async]: Serializer[F, ContractCreateRequested] =
    Serializer.lift(event => event.asJson.noSpaces.getBytes.pure[F])

  given serializerContractDeleteRequestedKey[F[_] : Async]: Serializer[F, ContractDeleteRequestedKey] =
    Serializer.lift(key => key.asJson.noSpaces.getBytes.pure[F])
  
  given serializerContractDeleteRequested[F[_] : Async]: Serializer[F, ContractDeleteRequested] =
    Serializer.lift(event => event.asJson.noSpaces.getBytes.pure[F])
