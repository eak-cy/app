package io.rikkos.gateway.it.client

import cats.syntax.all.*
import com.dimafeng.testcontainers.{DockerComposeContainer, ExposedService}
import fs2.io.net.Network
import io.rikkos.gateway.it.client.GatewayApiClient.GatewayApiClientConfig
import org.http4s.*
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import zio.*
import zio.interop.catz.*

final case class GatewayApiClient(config: GatewayApiClientConfig, client: Client[Task]) {
  import config.*

  def liveness: Task[Status] = client.get(healthUri / "liveness")(_.status.pure[Task])

  def readiness: Task[Status] = client.get(healthUri / "readiness")(_.status.pure[Task])
}

object GatewayApiClient {
  lazy val ServiceName = "gateway"
  lazy val ServicePort = 8080
  lazy val HealthPort  = 8081
  lazy val ExposedServices = Set(
    ExposedService(ServiceName, ServicePort),
    ExposedService(ServiceName, HealthPort),
  )

  final case class GatewayApiClientConfig(serviceUri: Uri, healthUri: Uri)

  def createClient(container: DockerComposeContainer)(using Network[Task]): ZIO[Scope, Throwable, GatewayApiClient] =
    for {
      host        = container.getServiceHost(ServiceName, ServicePort)
      servicePort = container.getServicePort(ServiceName, ServicePort)
      healthPort  = container.getServicePort(ServiceName, HealthPort)
      config = GatewayApiClientConfig(
        Uri.unsafeFromString(s"http://$host:$servicePort"),
        Uri.unsafeFromString(s"http://$host:$healthPort"),
      )
      client <- EmberClientBuilder
        .default[Task]
        .build
        .toScopedZIO
    } yield GatewayApiClient(config, client)

}
