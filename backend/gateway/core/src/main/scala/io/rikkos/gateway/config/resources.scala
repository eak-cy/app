package io.rikkos.gateway.config

import com.comcast.ip4s.*
import zio.config.magnolia.DeriveConfig

given DeriveConfig[Host] = DeriveConfig[String].mapAttempt(string => Host.fromString(string).get)

given DeriveConfig[Port] = DeriveConfig[Int].mapAttempt(int => Port.fromInt(int).get)
