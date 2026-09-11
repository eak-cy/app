package io.mesazon.gateway.golden

import io.mesazon.domain.gateway.*
import io.mesazon.gateway.clients.AIClient
import io.mesazon.gateway.config.AIClientConfig
import io.mesazon.gateway.json.given
import io.mesazon.gateway.service.FileService
import io.mesazon.gateway.utils.FileByteStreamScanned
import io.mesazon.testkit.base.ZWordSpecBase
import sttp.client4.httpclient.zio.HttpClientZioBackend
import zio.*
import zio.stream.ZStream

/** Manual-only check against the real OpenAI API: sends each sample photo in
  * `assets/contact-book-test-photo-1.png`..`-10.png` through the real `AIClient`, so a person can eyeball how well the
  * model actually extracts customers from a variety of real-looking source material (different languages, column
  * namings, source types).
  *
  * Never calls out for real in CI: `apiKey` ships empty, so every case cancels itself (reported as pending, not a
  * failure) rather than hitting the real API with a blank key. To run for real, fill in a real key below and invoke
  * this spec directly:
  * {{{
  * sbt "gateway-core/testOnly io.mesazon.gateway.golden.ExtractCustomersFromPhotoGoldenSpec"
  * }}}
  */
class ExtractCustomersFromPhotoGoldenSpec extends ZWordSpecBase {

  private val apiKey = ""

  private def buildAIClient: AIClient = ZIO
    .service[AIClient]
    .provide(
      AIClient.live,
      ZLayer.succeed(AIClientConfig(scheme = "https", host = "api.openai.com", port = 443, apiKey = apiKey)),
      HttpClientZioBackend.layer(),
    )
    .zioValue

  "AIClient" when {
    "extractFromImage" should {
      (1 to 10).foreach { photoNumber =>
        s"extract customers from contact-book-test-photo-$photoNumber.png" in {
          assume(
            apiKey.nonEmpty,
            "Fill in a real OpenAI API key in ExtractCustomersFromPhotoGoldenSpec.apiKey to run this manually",
          )

          val aiClient = buildAIClient

          val imageByteStream =
            FileByteStreamScanned(ZStream.fromResource(s"assets/contact-book-test-photo-$photoNumber.png"))

          val extractCustomersResponse = aiClient
            .extractFromImage[ExtractCustomersResponse](
              imageByteStream,
              SupportedMediaType.PNG,
              FileService.extractCustomersFromPhotoInstructions,
            )
            .zioValue

          info(s"contact-book-test-photo-$photoNumber.png => $extractCustomersResponse")

          extractCustomersResponse.entriesIdentified should be >= 0L
        }
      }
    }
  }
}
