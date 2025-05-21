package io.rikkos.domain

sealed abstract class AuthorizationError(
    val message: String,
    val underlying: Option[Throwable] = None,
) extends Exception(message, underlying.orNull)

object AuthorizationError {
  case object TokenMissing extends AuthorizationError("token is missing from request")

  final case class TokenFailedAuthorization(throwable: Throwable)
      extends AuthorizationError("token failed authorization", Some(throwable))
}
