package io.rikkos.gateway.repository

import io.rikkos.gateway.smithy.UserDetails
import zio.*

trait UserRepository {
  def onboard(user: UserDetails): UIO[UserDetails]
}

object UserRepository {
  val layer: ULayer[UserRepository] = ZLayer {
    Ref.make(Map.empty[Option[String], UserDetails]).map { ref =>
      new UserRepository {
        def onboard(user: UserDetails): UIO[UserDetails] =
          ref.updateAndGet(_.updated(user.email, user)).map(_ => user)
      }
    }
  }
}
