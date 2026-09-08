package io.mesazon.gateway.clients

import com.github.plokhotnyuk.jsoniter_scala.core.*
import io.mesazon.domain.gateway.ServiceError
import sttp.tapir.Schema
import zio.*
import zio.stream.*

trait AIClient {
  def extractFromImage[A](
      imageByteStream: ZStream[Any, Throwable, Byte],
      instructions: String,
  )(using Schema[A], JsonValueCodec[A]): IO[ServiceError, A]
}

object AIClient {

  private final class AIClientImpl extends AIClient {
    override def extractFromImage[A](
        imageByteStream: ZStream[Any, Throwable, Byte],
        instructions: String,
    )(using Schema[A], JsonValueCodec[A]): IO[ServiceError, A] =
      ZIO.die(new NotImplementedError("AIClient.extractFromImage is not yet implemented"))
  }

  val live = ZLayer.derive[AIClientImpl].project[AIClient](identity)
}
