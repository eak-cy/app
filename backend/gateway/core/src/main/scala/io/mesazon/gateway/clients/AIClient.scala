package io.mesazon.gateway.clients

import com.github.plokhotnyuk.jsoniter_scala.core.*
import io.mesazon.domain.gateway.{ServiceError, SupportedMediaType}
import io.mesazon.gateway.config.AIClientConfig
import io.mesazon.gateway.utils.FileByteStreamScanned
import sttp.ai.openai.OpenAI
import sttp.ai.openai.requests.completions.chat.ChatRequestBody.{ChatBody, ChatCompletionModel, ResponseFormat}
import sttp.ai.openai.requests.completions.chat.message.*
import sttp.apispec.{AnySchema, Schema as ApiSchema, SchemaLike, SchemaType}
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

  private[clients] def responseSchema[A](using schema: Schema[A]): ApiSchema =
    normalizeSchema(
      TapirSchemaToJsonSchema(
        schema,
        markOptionsAsNullable = true,
      )
    )

  private def normalizeSchemaLike(schemaLike: SchemaLike): SchemaLike = schemaLike match {
    case schema: ApiSchema    => normalizeSchema(schema)
    case anySchema: AnySchema => anySchema
  }

  private def normalizeSchema(schema: ApiSchema): ApiSchema = {
    val isObject = schema.`type`.exists(_.contains(SchemaType.Object))

    schema.copy(
      $schema = None,
      $defs = schema.$defs.map(_.map((name, nested) => name -> normalizeSchemaLike(nested))),
      default = None,
      allOf = schema.allOf.map(normalizeSchemaLike),
      anyOf = schema.anyOf.map(normalizeSchemaLike),
      oneOf = schema.oneOf.map(normalizeSchemaLike),
      not = schema.not.map(normalizeSchemaLike),
      `if` = schema.`if`.map(normalizeSchemaLike),
      `then` = schema.`then`.map(normalizeSchemaLike),
      `else` = schema.`else`.map(normalizeSchemaLike),
      dependentSchemas = schema.dependentSchemas.map((name, nested) => name -> normalizeSchemaLike(nested)),
      prefixItems = schema.prefixItems.map(_.map(normalizeSchemaLike)),
      items = schema.items.map(normalizeSchemaLike),
      contains = schema.contains.map(normalizeSchemaLike),
      unevaluatedItems = schema.unevaluatedItems.map(normalizeSchemaLike),
      required = if (isObject) schema.properties.keys.toList else schema.required,
      properties = schema.properties.map((name, nested) => name -> normalizeSchemaLike(nested)),
      patternProperties = schema.patternProperties.map((pattern, nested) => pattern -> normalizeSchemaLike(nested)),
      additionalProperties =
        if (isObject) Some(AnySchema.Nothing) else schema.additionalProperties.map(normalizeSchemaLike),
      propertyNames = schema.propertyNames.map(normalizeSchemaLike),
      unevaluatedProperties = schema.unevaluatedProperties.map(normalizeSchemaLike),
    )
  }

  private final class AIClientImpl(
      openAI: OpenAI,
      backend: Backend[Task],
  ) extends AIClient {

    private def responseFormat[A](using schema: Schema[A]): ResponseFormat.JsonSchema =
      ResponseFormat.JsonSchema(
        name = "ai_client_response",
        strict = Some(true),
        schema = Some(responseSchema[A]),
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
