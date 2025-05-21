package io.rikkos.gateway.middleware

import io.rikkos.gateway.auth.AuthorizationMiddleware
import org.http4s.HttpApp
import smithy4s.Hints
import smithy4s.http4s.ServerEndpointMiddleware
import zio.*

object ServerMiddleware {

  final private class ServerMiddlewareImpl(authorization: AuthorizationMiddleware)
      extends ServerEndpointMiddleware.Simple[Task] {

    override def prepareWithHints(serviceHints: Hints, endpointHints: Hints): HttpApp[Task] => HttpApp[Task] =
      serviceHints.get[smithy.api.HttpBearerAuth] match {
        case Some(_) =>
          endpointHints.get[smithy.api.Auth] match {
            case Some(auths) if auths.value.isEmpty => identity
            case _                                  => authorization.middleware
          }
        case None => identity
      }
  }

  val live = ZLayer(
    for {
      authorization <- ZIO.service[AuthorizationMiddleware]
    } yield new ServerMiddlewareImpl(authorization): ServerEndpointMiddleware.Simple[Task]
  )
}
