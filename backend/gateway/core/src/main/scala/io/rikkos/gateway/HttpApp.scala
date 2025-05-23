package io.rikkos.gateway

import fs2.io.net.Network
import io.rikkos.gateway.config.GatewayServerConfig
import io.rikkos.gateway.config.GatewayServerConfig.ServerConfig
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.EntityLimiter
import smithy4s.http4s.SimpleRestJsonBuilder
import zio.*
import zio.interop.catz.*

object HttpApp {

  given Network[Task] = Network.forAsync[Task]
  private val dsl     = Http4sDsl[Task]
  import dsl.*

  private val healthRoutesResource = for {
    probes <- ZIO.service[smithy.HealthCheckService[Task]]
    routes <- SimpleRestJsonBuilder.routes(probes).resource.toScopedZIO
  } yield routes

  private val serviceRoutesResource = for {
    userManagementService <- ZIO.service[smithy.UserManagementService[Task]]
    userManagementRoutes  <- SimpleRestJsonBuilder.routes(userManagementService).resource.toScopedZIO
  } yield userManagementRoutes

  private def server(config: ServerConfig, routes: HttpRoutes[Task]) = for {
    emberServer <- EmberServerBuilder
      .default[Task]
      .withHost(config.host)
      .withPort(config.port)
      .withHttpApp(EntityLimiter.httpRoutes(routes).orNotFound) // Limits the size of the request body to 2MB
      .withErrorHandler(error =>
        ZIO.fiberId.flatMap(fid =>
          ZIO.logErrorCause("Unexpected failure", Cause.die(error, StackTrace.fromJava(fid, error.getStackTrace)))
        ) *> InternalServerError()
      )
      .build
      .toScopedZIO
  } yield emberServer

  val serverLayer = ZLayer.scoped {
    (for {
      config        <- ZIO.service[GatewayServerConfig]
      healthRoutes  <- healthRoutesResource
      serviceRoutes <- serviceRoutesResource
      servers       <- server(config.health, healthRoutes) &> server(config.service, serviceRoutes)
    } yield servers).forkScoped
  }
}
