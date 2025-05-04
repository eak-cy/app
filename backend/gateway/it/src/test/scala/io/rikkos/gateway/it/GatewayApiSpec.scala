package io.rikkos.gateway.it

import com.dimafeng.testcontainers.DockerComposeContainer
import fs2.io.net.Network
import io.rikkos.gateway.it.GatewayApiSpec.Context
import io.rikkos.gateway.it.client.GatewayApiClient
import io.rikkos.testkit.base.{DockerComposeBase, ZWordSpecBase}
import org.http4s.Status
import zio.*
import zio.interop.catz.*

class GatewayApiSpec extends ZWordSpecBase with DockerComposeBase {

  given Network[Task] = Network.forAsync[Task]

  override def exposedServices = GatewayApiClient.ExposedServices

  def withContext[A](f: Context => A): A = ZIO
    .scoped(withContainers { container =>
      for {
        dcContainer = container.asInstanceOf[DockerComposeContainer]
        gatewayApiClient <- GatewayApiClient.createClient(dcContainer)
      } yield f(Context(gatewayApiClient))
    })
    .zioValue

  "GatewayApi" when {
    "/liveness" should {
      "return successfully when service is live" in
        withContext { case Context(gatewayApiClient) =>
          eventually {
            gatewayApiClient.liveness.zioValue shouldBe Status.NoContent
          }
        }
    }

    "/readiness" should {
      "return successfully when service is ready" in
        withContext { case Context(gatewayApiClient) =>
          eventually {
            gatewayApiClient.readiness.zioValue shouldBe Status.NoContent
          }
        }
    }
  }
}

object GatewayApiSpec {

  final case class Context(apiClient: GatewayApiClient)
}
