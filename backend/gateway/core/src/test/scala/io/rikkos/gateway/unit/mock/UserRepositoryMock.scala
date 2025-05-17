package io.rikkos.gateway.unit.mock

import io.rikkos.domain.UserDetails
import io.rikkos.gateway.repository.UserRepository
import zio.*

final class UserRepositoryMock(maybeError: Option[Throwable] = None) extends UserRepository {

  override def insertUserDetails(userDetails: UserDetails): UIO[Unit] = maybeError match {
    case Some(error) => ZIO.fail(error).orDie
    case None        => ZIO.unit
  }
}
