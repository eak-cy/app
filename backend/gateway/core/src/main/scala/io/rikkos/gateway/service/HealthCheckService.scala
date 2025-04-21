package io.rikkos.gateway.service

import io.rikkos.gateway.smithy
import zio.*

object HealthCheckService {

  final private class HealthCheckServiceImpl extends smithy.HealthCheckService[Task] {
    override def liveness(): Task[Unit] = ZIO.unit

    override def readiness(): Task[Unit] = ZIO.unit
  }

  val live = ZLayer.succeed(new HealthCheckServiceImpl(): smithy.HealthCheckService[Task])
}
