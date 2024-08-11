package io.github.sergeiionin.contractsregistrator
package serverendpoints

import cats.effect.IO
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

final class SwaggerServerEndpoints(endpoints: List[AnyEndpoint]):
    def getSwaggerUIServerEndpoints(): List[ServerEndpoint[Any, IO]] =
      SwaggerInterpreter().fromEndpoints[IO](endpoints, "Contracts Registry Endpoints", "0.0.1")
