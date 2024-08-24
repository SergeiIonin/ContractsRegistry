package io.github.sergeiionin.contractsregistrator
package serverendpoints

import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import sttp.tapir.swagger.bundle.SwaggerInterpreter

final class SwaggerServerEndpoints[F[_]](endpoints: List[AnyEndpoint]):
    def getSwaggerUIServerEndpoints(): List[ServerEndpoint[Any, F]] =
      SwaggerInterpreter().fromEndpoints[F](endpoints, "Contracts Registry Endpoints", "0.0.1")
