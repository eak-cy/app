package io.rikkos.testkit.base

import zio.*
import zio.config.typesafe.TypesafeConfigProvider
import zio.logging.backend.SLF4J

trait ZIOTestOps {

  private val loggerLayer = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val configProvider = TypesafeConfigProvider.fromResourcePath()

  implicit class ZIOOps[E, A](private val zio: IO[E, A]) {

    def zioEither: Either[E, A] =
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe
          .run[Nothing, Either[E, A]](
            zio
              .provideLayer(loggerLayer)
              .withConfigProvider(configProvider)
              .either
          )
          .getOrThrowFiberFailure()
      }

    def zioValue: A =
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe
          .run[E, A](
            zio.provideLayer(loggerLayer).withConfigProvider(configProvider)
          )
          .getOrThrowFiberFailure()
      }
  }

  implicit class ZIORefOps[A](private val ref: Ref[A]) {

    def refValue: A =
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.run(ref.get).getOrThrowFiberFailure()
      }
  }
}
