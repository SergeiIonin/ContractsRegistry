package io.github.sergeiionin.contractsregistrator

import config.RestApiApplicationConfig
import http.client.ContractsRegistryHttpClient
import producer.GitHubEventsProducer
import domain.events.prs.{PrClosed, PrClosedKey}
import serverendpoints.{ContractsServerEndpoints, SwaggerServerEndpoints, WebhooksPrsServerEndpoints}

import cats.effect.{ExitCode, IO, IOApp}
import com.comcast.ip4s.{Host, Port, port}
import fs2.kafka.{KafkaProducer, ProducerSettings, Serializer}
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}
import sttp.tapir.server.http4s.Http4sServerInterpreter
import io.circe.syntax.* 
import domain.events.prs.given

object Main extends IOApp:
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  
  val config = RestApiApplicationConfig.load
  val host = config.restApi.host
  val port = config.restApi.port
  val baseClientUri = s"${config.schemaRegistry.host}:${config.schemaRegistry.port}"
  
  val producerTopic = config.kafkaProducer.prsTopic
  
  given Serializer[IO, PrClosedKey] = Serializer.lift(key => IO.pure(key.asJson.noSpaces.getBytes))
  given Serializer[IO, PrClosed] = Serializer.lift(event => IO.pure(event.asJson.noSpaces.getBytes))

  val producerSettings: ProducerSettings[IO, PrClosedKey, PrClosed] =
    ProducerSettings[IO, PrClosedKey, PrClosed](
      Serializer.apply[IO, PrClosedKey],
      Serializer.apply[IO, PrClosed],
    ).withBootstrapServers(config.kafkaProducer.bootstrapServers.head)
  
  def run(args: List[String]): IO[ExitCode] =
    (for
      contractsClient           <- ContractsRegistryHttpClient.make[IO]()
      kafkaProducer             <- KafkaProducer[IO].resource[PrClosedKey, PrClosed](producerSettings)
      producer                  <- GitHubEventsProducer.makePRsProducer(producerTopic, kafkaProducer)
      contractsServerEndpoints  = ContractsServerEndpoints[IO](baseClientUri, contractsClient)
      webhooksServerEndpoints   = WebhooksPrsServerEndpoints[IO](producer)
      serverEndpoints           = contractsServerEndpoints.serverEndpoints ++ webhooksServerEndpoints.serverEndpoints
      swaggerServerEndpoints    = SwaggerServerEndpoints(contractsServerEndpoints.getEndpoints ++
                                          webhooksServerEndpoints.getEndpoints)
                                            .getSwaggerUIServerEndpoints()
      routes                    = Http4sServerInterpreter[IO]().toRoutes(serverEndpoints ++ swaggerServerEndpoints)
      server                    <- EmberServerBuilder
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