package io.mesazon.gateway.it

import com.dimafeng.testcontainers.ExposedService
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import io.mesazon.domain.gateway.*
import io.mesazon.gateway.clients.AIClient
import io.mesazon.gateway.config.AIClientConfig
import io.mesazon.gateway.utils.FileByteStreamScanned
import io.mesazon.testkit.base.*
import io.mesazon.wiremock.WiremockClient
import io.mesazon.wiremock.WiremockClient.WiremockClientConfig
import sttp.client4.httpclient.zio.HttpClientZioBackend
import sttp.model.StatusCode
import sttp.tapir.Schema
import zio.*
import zio.stream.*

class AIClientSpec extends ZWordSpecBase, DockerComposeBase {

  override def dockerComposeFile: String = "./src/test/resources/compose/wiremock.yaml"

  override def exposedServices: Set[ExposedService] = WiremockClient.ExposedServices

  case class ExtractedTestResult(value: String)

  given Schema[ExtractedTestResult]         = Schema.derived[ExtractedTestResult]
  given JsonValueCodec[ExtractedTestResult] = JsonCodecMaker.make[ExtractedTestResult]

  case class Context(aiClientConfig: AIClientConfig, wiremockClient: WiremockClient)

  def withContext[A](f: Context => A): A = withContainers { container =>
    val wiremockClientConfig = WiremockClientConfig.from(container)
    val wiremockClient       = ZIO
      .service[WiremockClient]
      .provide(
        WiremockClient.live,
        ZLayer.succeed(wiremockClientConfig),
        HttpClientZioBackend.layer(),
      )
      .zioValue

    val aiClientConfig = AIClientConfig(
      scheme = "http",
      host = wiremockClientConfig.host,
      port = wiremockClientConfig.port,
      apiKey = "test-api-key",
    )

    f(Context(aiClientConfig, wiremockClient))
  }

  override def beforeAll(): Unit = withContext { context =>
    import context.*

    super.beforeAll()

    eventually {
      val healthCheckStatusResponse = wiremockClient.healthCheck.zioValue

      healthCheckStatusResponse.code shouldBe StatusCode.Ok
      healthCheckStatusResponse.body.status shouldBe "healthy"
    }
  }

  override def afterEach(): Unit = withContext { context =>
    import context.*

    super.afterEach()

    eventually {
      wiremockClient.reset.zioValue.code shouldBe StatusCode.Ok
    }
  }

  "AIClient" when {
    "extractFromImage" should {
      "successfully extract a structured response from a photo" in withContext { context =>
        import context.*

        val aiClient = ZIO
          .service[AIClient]
          .provide(
            AIClient.live,
            ZLayer.succeed(aiClientConfig),
            HttpClientZioBackend.layer(),
          )
          .zioValue

        val imageByteStream = FileByteStreamScanned(ZStream.fromIterable(Array[Byte](1, 2, 3, 4, 5)))

        val extractedTestResult = aiClient
          .extractFromImage[ExtractedTestResult](imageByteStream, SupportedMediaType.JPEG, "AI_CLIENT_SPEC_SUCCESS")
          .zioValue

        extractedTestResult shouldBe ExtractedTestResult("extracted-value")

        val requestMappings =
          wiremockClient.requestsDetails.zioValue.filter(_.count > 0).sortBy(_.lastCallDate)

        requestMappings.size shouldBe 1

        requestMappings(0).mapping.method shouldBe "POST"
        requestMappings(0).mapping.url shouldBe "/v1/chat/completions"
        requestMappings(0).count shouldBe 1
      }

      "fail with an UnexpectedError when the AI service returns an error" in withContext { context =>
        import context.*

        val aiClient = ZIO
          .service[AIClient]
          .provide(
            AIClient.live,
            ZLayer.succeed(aiClientConfig),
            HttpClientZioBackend.layer(),
          )
          .zioValue

        val imageByteStream = FileByteStreamScanned(ZStream.fromIterable(Array[Byte](1, 2, 3, 4, 5)))

        val serviceError = aiClient
          .extractFromImage[ExtractedTestResult](imageByteStream, SupportedMediaType.JPEG, "AI_CLIENT_SPEC_ERROR")
          .zioError

        serviceError shouldBe a[ServiceError.InternalServerError.UnexpectedError]
        serviceError.message shouldBe "Unable to send message to AI"
      }

      "fail with an UnexpectedError when the AI service returns undecodable structured-output JSON" in withContext {
        context =>
          import context.*

          val aiClient = ZIO
            .service[AIClient]
            .provide(
              AIClient.live,
              ZLayer.succeed(aiClientConfig),
              HttpClientZioBackend.layer(),
            )
            .zioValue

          val imageByteStream = FileByteStreamScanned(ZStream.fromIterable(Array[Byte](1, 2, 3, 4, 5)))

          val serviceError = aiClient
            .extractFromImage[ExtractedTestResult](
              imageByteStream,
              SupportedMediaType.JPEG,
              "AI_CLIENT_SPEC_MALFORMED",
            )
            .zioError

          serviceError shouldBe a[ServiceError.InternalServerError.UnexpectedError]
          serviceError.message should startWith("Failed to parse AI response")
      }
    }
  }
}
