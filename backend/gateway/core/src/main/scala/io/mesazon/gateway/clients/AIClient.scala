package io.mesazon.gateway.clients

import com.github.plokhotnyuk.jsoniter_scala.core.*
import io.mesazon.domain.gateway.{ServiceError, SupportedMediaType}
import io.mesazon.gateway.config.AIClientConfig
import io.mesazon.gateway.utils.FileByteStreamScanned
import sttp.ai.openai.OpenAI
import sttp.ai.openai.requests.completions.chat.ChatRequestBody.{ChatBody, ChatCompletionModel, ResponseFormat}
import sttp.ai.openai.requests.completions.chat.message.*
import sttp.client4.Backend
import sttp.tapir.Schema
import sttp.tapir.docs.apispec.schema.TapirSchemaToJsonSchema
import zio.*

import java.util.Base64

trait AIClient {
  def extractFromImage[A](
      imageByteStream: FileByteStreamScanned,
      supportedMediaType: SupportedMediaType,
      instructions: String,
  )(using Schema[A], JsonValueCodec[A]): IO[ServiceError, A]
}

object AIClient {

  private final class AIClientImpl(
      openAI: OpenAI,
      backend: Backend[Task],
  ) extends AIClient {

    private def responseFormat[A](using schema: Schema[A]): ResponseFormat.JsonSchema =
      ResponseFormat.JsonSchema(
        name = "ai_client_response",
        strict = Some(true),
        schema = Some(
          TapirSchemaToJsonSchema(
            schema,
            markOptionsAsNullable = true,
          )
        ),
        description = None,
      )

    override def extractFromImage[A](
        imageByteStream: FileByteStreamScanned,
        supportedMediaType: SupportedMediaType,
        instructions: String,
    )(using Schema[A], JsonValueCodec[A]): IO[ServiceError, A] =
      for {
        imageBytes <- imageByteStream.value.runCollect
          .map(_.toArray)
          .mapError(error =>
            ServiceError.InternalServerError.UnexpectedError("Failed to read image for AI extraction", Some(error))
          )
        imageBase64 = Base64.getEncoder.encodeToString(imageBytes)
        response <- openAI
          .createChatCompletion(
            ChatBody(
              model = ChatCompletionModel.GPT56Sol,
              messages = Seq(
                Message.System(instructions),
                Message.User(
                  Content.ArrayContent(
                    Seq(
                      Content.ContentPart.ImageUrl(
                        Content.ImageUrlDetails(url = s"data:${supportedMediaType.mime};base64,$imageBase64")
                      )
                    )
                  )
                ),
              ),
              responseFormat = Some(responseFormat),
            )
          )
          .send(backend)
          .map(_.body)
          .absolve
          .mapError(error =>
            ServiceError.InternalServerError.UnexpectedError("Unable to send message to AI", Some(error))
          )
        result <- ZIO
          .attempt(readFromString[A](response.choices.head.message.content))
          .mapError(error =>
            ServiceError.InternalServerError
              .UnexpectedError(s"Failed to parse AI response ${response.choices.mkString("\n")}", Some(error))
          )
      } yield result
  }

  private def observed(client: AIClient): AIClient = client

  val live = ZLayer(
    ZIO.service[AIClientConfig].map(aiClientConfig => new OpenAI(aiClientConfig.apiKey, aiClientConfig.baseUri))
  ) >>> ZLayer.derive[AIClientImpl] >>> ZLayer.fromFunction(observed)
}
