package io.mesazon.gateway.mock

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import io.mesazon.domain.gateway.{ServiceError, SupportedMediaType}
import io.mesazon.gateway.clients.AIClient
import io.mesazon.gateway.utils.FileByteStreamScanned
import sttp.tapir.Schema
import zio.*

object Mocks {

  final class AIClientMock[A](
      extractFromImageResult: IO[ServiceError, A],
      val extractFromImageCallsRef: Ref[List[(FileByteStreamScanned, SupportedMediaType, String)]],
  ) extends AIClient {
    override def extractFromImage[B](
        imageByteStream: FileByteStreamScanned,
        supportedMediaType: SupportedMediaType,
        instructions: String,
    )(using Schema[B], JsonValueCodec[B]): IO[ServiceError, B] =
      extractFromImageCallsRef.update(_ :+ (imageByteStream, supportedMediaType, instructions)) *>
        extractFromImageResult.map(_.asInstanceOf[B])
  }
}
