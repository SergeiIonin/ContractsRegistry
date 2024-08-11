package io.github.sergeiionin.contractsregistrator

import cats.effect.{ExitCode, IO, IOApp}
import org.http4s.ember.client.EmberClientBuilder
import io.github.sergeiionin.contractsregistrator.http.client.ContractsRegistryHttpClient
import io.github.sergeiionin.contractsregistrator.serverendpoints.ContractsServerEndpoints
import io.github.sergeiionin.contractsregistrator.serverendpoints.SwaggerServerEndpoints
import sttp.tapir.server.http4s.Http4sServerInterpreter
import org.http4s.ember.server.EmberServerBuilder
import com.comcast.ip4s.{Host, Port, port}

object Main extends IOApp:
  // fixme add config
  val host = "http://localhost"
  val port = 8080
  val baseClientUri = s"http://localhost:8081"
  
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