package io.github.sergeiionin.contractsregistrator

import client.{CreateSchemaClient, DeleteSchemaClient, GetSchemaClient}
import config.RestApiApplicationConfig
import domain.events.contracts.{ContractDeleteRequested, ContractDeleteRequestedKey}
import http.client.HttpClient
import producer.EventsProducer
import producer.KafkaEventsProducer.given
import producer.contracts.{ContractCreateKafkaEventsProducer, ContractDeleteKafkaEventsProducer}
import repository.ContractsRepository
import serverendpoints.{
  CreateContractServerEndpoints,
  DeleteContractServerEndpoints,
  GetContractServerEndpoints,
  SwaggerServerEndpoints,
  WebhooksPrsServerEndpoints
}
import service.prs.PrService
import service.ContractService

import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.option.*
import com.comcast.ip4s.{Host, Port, port}
import fs2.kafka.{KafkaProducer, ProducerSettings, Serializer}
import io.circe.syntax.*
import natchez.Trace.Implicits.noop
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}
import skunk.Session
import sttp.tapir.server.http4s.Http4sServerInterpreter

object Main extends IOApp:
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]

  val config = RestApiApplicationConfig.load
  val host = config.restApi.host
  val port = config.restApi.port
  val baseClientUri = s"${config.schemaRegistry.host}:${config.schemaRegistry.port}"

  val postgres = config.postgres

  val contractsDeletedTopic = config.kafkaProducer.contractsDeletedTopic
  val contractsCreatedTopic = config.kafkaProducer.contractsCreatedTopic

  val bootstrapServers = config.kafkaProducer.bootstrapServers.head

  // todo add get endpoints
  def run(args: List[String]): IO[ExitCode] =
    (for
      session <- Session.pooled[IO](
        host = postgres.host,
        port = postgres.port,
        user = postgres.user,
        database = postgres.database,
        password = postgres.password.some,
        max = 10)
      httpClient          <- HttpClient.make[IO]()
      createSchemaClient  <- CreateSchemaClient.make[IO](baseClientUri, httpClient)
      getSchemaClient     <- GetSchemaClient.make[IO](baseClientUri, httpClient)
      deleteSchemaClient  <- DeleteSchemaClient.make[IO](baseClientUri, httpClient)
      contractsRepository <- ContractsRepository.make[IO](session)
      contractService     <- ContractService.make[IO](contractsRepository)
      prsService <- PrService.make[IO](contractService, getSchemaClient, deleteSchemaClient)
      deleteKafkaEventsProducer <- ContractDeleteKafkaEventsProducer.make[IO](
        contractsDeletedTopic,
        bootstrapServers)
      createKafkaEventsProducer <- ContractCreateKafkaEventsProducer.make[IO](
        contractsCreatedTopic,
        bootstrapServers)
      createContractsServerEndpoints = CreateContractServerEndpoints[IO](
        createSchemaClient,
        getSchemaClient,
        createKafkaEventsProducer)
      getContractsServerEndpoints = GetContractServerEndpoints[IO](contractService)
      deleteContractsServerEndpoints = DeleteContractServerEndpoints[IO](
        getSchemaClient,
        deleteKafkaEventsProducer)
      webhooksServerEndpoints = WebhooksPrsServerEndpoints[IO](prsService)
      serverEndpoints = createContractsServerEndpoints.serverEndpoints ++
        getContractsServerEndpoints.serverEndpoints ++
        deleteContractsServerEndpoints.serverEndpoints ++
        webhooksServerEndpoints.serverEndpoints
      swaggerServerEndpoints = SwaggerServerEndpoints[IO](
        createContractsServerEndpoints.getEndpoints ++
          getContractsServerEndpoints.getEndpoints ++
          deleteContractsServerEndpoints.getEndpoints ++
          webhooksServerEndpoints.getEndpoints
      ).getSwaggerUIServerEndpoints()
      routes = Http4sServerInterpreter[IO]().toRoutes(serverEndpoints ++ swaggerServerEndpoints)
      server <- EmberServerBuilder
        .default[IO]
        .withHost(Host.fromString("localhost").get)
        .withPort(Port.fromInt(port).get)
        .withHttpApp(routes.orNotFound)
        .build
    yield server)
      .use { _ =>
        IO.println(s"Server started on $host:$port") *>
          IO.println(s"View REST API reference at $host:$port/docs") *>
          IO.never
      }
      .as(ExitCode.Success)
