package io.github.sergeiionin.contractsregistrator

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.ember.client.EmberClientBuilder
import http.client.ContractsRegistryHttpClient
import serverendpoints.ContractsServerEndpoints
import serverendpoints.SwaggerServerEndpoints
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.{Host, Port, port}
import config.RestApiApplicationConfig

object Main extends IOApp:
  val config = RestApiApplicationConfig.load
  val host = config.restApi.host
  val port = config.restApi.port
  val baseClientUri = s"${config.schemaRegistry.host}:${config.schemaRegistry.port}"
  
  def run(args: List[String]): IO[ExitCode] =
    (for
      contractsClient           <- ContractsRegistryHttpClient.make[IO]()
      contractsServerEndpoints  = ContractsServerEndpoints[IO](baseClientUri, contractsClient)
      serverEndpoints           = contractsServerEndpoints.serverEndpoints
      swaggerServerEndpoints    = SwaggerServerEndpoints(contractsServerEndpoints.getEndpoints).getSwaggerUIServerEndpoints()
      routes                    = Http4sServerInterpreter[IO]().toRoutes(serverEndpoints ++ swaggerServerEndpoints)
      server                    <- EmberServerBuilder
                                      .default[IO]
                                      .withHost(Host.fromString("localhost").get)
                                      .withPort(Port.fromInt(port).get)
                                      .withHttpApp(routes.orNotFound)
                                      .build
    yield server).use { _ =>
      IO.println(s"Server started on $host:$port") *>
      IO.never
    }.as(ExitCode.Success)