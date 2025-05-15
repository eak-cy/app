package io.rikkos.gateway.repository

import io.rikkos.domain.UserDetails
import zio.*

import java.util.UUID

trait UserRepository {
  def insertUserDetails(userDetails: UserDetails): UIO[Unit]
}

object UserRepository {

  final private class UserRepositoryMemory(userDetailsRef: Ref[Map[UUID, UserDetails]]) extends UserRepository {
    override def insertUserDetails(userDetails: UserDetails): UIO[Unit] = for {
      uuid <- ZIO.succeed(UUID.randomUUID())
      _    <- userDetailsRef.update(_.updated(uuid, userDetails))
    } yield ()
  }

  val layer: ULayer[UserRepository] = ZLayer {
    Ref.make(Map.empty[UUID, UserDetails]).map { userDetailsRef =>
      new UserRepositoryMemory(userDetailsRef)
    }
  }
}
