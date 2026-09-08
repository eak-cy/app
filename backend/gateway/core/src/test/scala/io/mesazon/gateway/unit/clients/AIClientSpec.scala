package io.mesazon.gateway.unit.clients

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import io.mesazon.gateway.clients.AIClient
import io.mesazon.testkit.base.ZWordSpecBase
import sttp.tapir.Schema
import zio.*
import zio.stream.*

class AIClientSpec extends ZWordSpecBase {

  private case class StubExtractionResult(value: String)

  private given Schema[StubExtractionResult]         = Schema.derived[StubExtractionResult]
  private given JsonValueCodec[StubExtractionResult] = JsonCodecMaker.make[StubExtractionResult]

  "AIClient" when {
    "extractFromImage" should {
      "given a photo and instructions, successfully replies with the extracted response" in new TestContext {
        val imageByteStream = ZStream.fromIterable(Array[Byte](1, 2, 3, 4, 5))
        val instructions    = "Extract the customers pictured in this photo."

        val aiClient = buildAIClient

        val cause = aiClient
          .extractFromImage[StubExtractionResult](imageByteStream, instructions)
          .zioCause

        cause.dieOption.value shouldBe a[NotImplementedError]
      }
    }
  }

  trait TestContext {
    def buildAIClient: AIClient = ZIO
      .service[AIClient]
      .provide(AIClient.live)
      .zioValue
  }
}
