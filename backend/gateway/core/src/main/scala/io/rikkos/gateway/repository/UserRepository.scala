package io.rikkos.gateway.repository

import io.rikkos.domain.UserDetails
import zio.*

import java.util.UUID

trait UserRepository {
  def insertUserDetails(userDetails: UserDetails): UIO[Unit]
}

object UserRepository {
  val layer: ULayer[UserRepository] = ZLayer {
    Ref.make(Map.empty[UUID, UserDetails]).map { ref =>
      new UserRepository {
        override def insertUserDetails(userDetails: UserDetails): UIO[Unit] =
          for {
            uuid <- ZIO.succeed(UUID.randomUUID())
            _    <- ref.update(_.updated(uuid, userDetails))
          } yield ()
      }
    }
  }
}
