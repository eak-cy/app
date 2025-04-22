package io.rikkos.gateway.config

import com.comcast.ip4s.*
import io.rikkos.gateway.config.GatewayServerConfig.ServerConfig
import zio.config.*
import zio.config.magnolia.*

final case class GatewayServerConfig(service: ServerConfig, health: ServerConfig)

object GatewayServerConfig {
  final case class ServerConfig(host: Host, port: Port)

  val live = deriveConfigLayer[GatewayServerConfig]("server")
}
