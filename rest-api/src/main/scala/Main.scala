package io.github.sergeiionin.contractsregistrator

import client.{CreateSchemaClient, DeleteSchemaClient}
import config.RestApiApplicationConfig
import domain.events.contracts.{ContractDeleteRequested, ContractDeleteRequestedKey}
import domain.events.prs.{PrClosed, PrClosedKey, given}
import http.client.HttpClient
import producer.EventsProducer
import producer.contracts.ContractDeleteEventsKafkaProducer
import producer.prs.PrEventsKafkaProducer
import serverendpoints.{CreateContractServerEndpoints, DeleteContractServerEndpoints, SwaggerServerEndpoints, WebhooksPrsServerEndpoints}

import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Host, Port, port}
import fs2.kafka.{KafkaProducer, ProducerSettings, Serializer}
import io.circe.syntax.*
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}
import sttp.tapir.server.http4s.Http4sServerInterpreter
import producer.EventsKafkaProducer.given

object Main extends IOApp:
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  
  val config = RestApiApplicationConfig.load
  val host = config.restApi.host
  val port = config.restApi.port
  val baseClientUri = s"${config.schemaRegistry.host}:${config.schemaRegistry.port}"
  
  val prsTopic = config.kafkaProducer.prsTopic
  val contractsDeletedTopic = config.kafkaProducer.contractsDeletedTopic
  
  val bootstrapServers = config.kafkaProducer.bootstrapServers.head
  
  // todo add get endpoints
  def run(args: List[String]): IO[ExitCode] =
    (for
      httpClient                      <- HttpClient.make[IO]()
      createSchemaClient              <- CreateSchemaClient.make[IO](baseClientUri, httpClient)
      kafkaPrsProducer                <- PrEventsKafkaProducer.make[IO](prsTopic, bootstrapServers)
      kafkaContractsProducer          <- ContractDeleteEventsKafkaProducer.make[IO](contractsDeletedTopic, bootstrapServers)
      createContractsServerEndpoints  = CreateContractServerEndpoints[IO](createSchemaClient)
      deleteContractsServerEndpoints  = DeleteContractServerEndpoints[IO](kafkaContractsProducer)
      webhooksServerEndpoints         = WebhooksPrsServerEndpoints[IO](kafkaPrsProducer)
      serverEndpoints                 = createContractsServerEndpoints.serverEndpoints ++
                                          deleteContractsServerEndpoints.serverEndpoints ++
                                          webhooksServerEndpoints.serverEndpoints
      swaggerServerEndpoints          = SwaggerServerEndpoints[IO](
                                                createContractsServerEndpoints.getEndpoints ++
                                                deleteContractsServerEndpoints.getEndpoints ++
                                                webhooksServerEndpoints.getEndpoints
                                        )
                                                  .getSwaggerUIServerEndpoints()
      routes                          = Http4sServerInterpreter[IO]().toRoutes(serverEndpoints ++ swaggerServerEndpoints)
      server                          <- EmberServerBuilder
                                            .default[IO]
                                            .withHost(Host.fromString("localhost").get)
                                            .withPort(Port.fromInt(port).get)
                                            .withHttpApp(routes.orNotFound)
                                            .build
    yield server).use { _ =>
          IO.println(s"Server started on $host:$port") *>
          IO.println(s"View REST API reference at $host:$port/docs") *>
          IO.never
    }.as(ExitCode.Success)