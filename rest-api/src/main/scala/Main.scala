package io.github.sergeiionin.contractsregistrator

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.ember.client.EmberClientBuilder
import http.client.ContractsRegistryHttpClient
import serverendpoints.{ContractsServerEndpoints, WebhooksServerEndpoints}
import serverendpoints.SwaggerServerEndpoints
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.{Host, Port, port}
import config.RestApiApplicationConfig
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.{Logger, LoggerFactory}

object Main extends IOApp:
  given logger: Logger[IO] = Slf4jLogger.getLogger[IO]
  
  val config = RestApiApplicationConfig.load
  val host = config.restApi.host
  val port = config.restApi.port
  val baseClientUri = s"${config.schemaRegistry.host}:${config.schemaRegistry.port}"
  
  def run(args: List[String]): IO[ExitCode] =
    (for
      contractsClient           <- ContractsRegistryHttpClient.make[IO]()
      contractsServerEndpoints  = ContractsServerEndpoints[IO](baseClientUri, contractsClient)
      webhooksServerEndpoints   = WebhooksServerEndpoints[IO]()
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