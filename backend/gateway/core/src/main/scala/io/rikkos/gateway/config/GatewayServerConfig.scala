package io.rikkos.gateway.config

import com.comcast.ip4s.*
import io.rikkos.domain.AppName
import io.rikkos.gateway.config.GatewayServerConfig.ServerConfig
import zio.config.*
import zio.config.magnolia.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.Config
import zio.ConfigProvider
import zio.ZIO
import zio.ZLayer

final case class GatewayServerConfig(service: ServerConfig, health: ServerConfig)

object GatewayServerConfig {

  final case class ServerConfig(host: Host, port: Port)

  private def config(appName: AppName): Config[GatewayServerConfig] =
    deriveConfig[GatewayServerConfig].nested(s"$appName", "server").mapKey(toKebabCase)

  val live = ZLayer(
    ZIO
      .service[AppName]
      .map(config)
      .flatMap(TypesafeConfigProvider.fromResourcePath().load)
  )
}
