package io.rikkos.gateway.auth

import io.rikkos.domain.{AuthMember, AuthorizationError, Email, MemberID}
import zio.*

trait AuthorizationService {
  def auth(token: String): IO[AuthorizationError.TokenFailedAuthorization, AuthMember]
}

object AuthorizationService {

  final private class AuthorizationServiceImpl() extends AuthorizationService {
    override def auth(token: String): IO[AuthorizationError.TokenFailedAuthorization, AuthMember] =
      ZIO.succeed(AuthMember(MemberID.assume("test"), Email.assume("eliot.martel@gmail.com")))
  }

//  val live: ULayer[AuthorizationServiceImpl] = ZLayer.succeed(new AuthorizationServiceImpl())
}
