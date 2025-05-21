package io.rikkos.gateway.auth

import io.rikkos.domain.AuthorizationError
import io.rikkos.gateway.smithy
import org.http4s.*
import org.http4s.headers.Authorization
import zio.*
import zio.interop.catz.*

trait AuthorizationMiddleware {
  def middleware: HttpApp[Task] => HttpApp[Task]
}

object AuthorizationMiddleware {

  final private class AuthorizationMiddlewareImpl(service: AuthorizationService, state: AuthorizationState)
      extends AuthorizationMiddleware {
    override def middleware: HttpApp[Task] => HttpApp[Task] = { inputApp =>
      HttpApp[Task] { request =>
        (for {
          maybeBearerToken = request.headers
            .get[`Authorization`]
            .collect { case Authorization(Credentials.Token(AuthScheme.Bearer, token)) => token }
          bearerToken <- ZIO.fromOption(maybeBearerToken).mapError(_ => AuthorizationError.TokenMissing)
          authMember  <- service.auth(bearerToken)
          _           <- state.set(authMember)
        } yield ())
          .flatMapError(authError => ZIO.logErrorCause("Failed to authorize request", Cause.fail(authError)))
          .mapError(_ => smithy.Unauthorized()) *> inputApp(request)
      }
    }
  }

  val live = ZLayer {
    for {
      service <- ZIO.service[AuthorizationService]
      state   <- ZIO.service[AuthorizationState]
    } yield new AuthorizationMiddlewareImpl(service, state): AuthorizationMiddleware
  }
}
