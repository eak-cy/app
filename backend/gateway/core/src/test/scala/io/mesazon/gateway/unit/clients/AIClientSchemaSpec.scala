package io.mesazon.gateway.clients

import io.mesazon.domain.gateway.ExtractCustomersResponse
import io.mesazon.gateway.json.given
import io.mesazon.testkit.base.WordSpecBase
import sttp.apispec.{AnySchema, Schema as ApiSchema, SchemaLike, SchemaType}

class AIClientSchemaSpec extends WordSpecBase {

  private val responseSchema = AIClient.responseSchema[ExtractCustomersResponse]

  private def schemasIn(schemaLike: SchemaLike): List[ApiSchema] = schemaLike match {
    case schema: ApiSchema =>
      val children =
        schema.$defs.toList.flatMap(_.values) ++
          schema.allOf ++
          schema.anyOf ++
          schema.oneOf ++
          schema.not ++
          schema.`if` ++
          schema.`then` ++
          schema.`else` ++
          schema.dependentSchemas.values ++
          schema.prefixItems.toList.flatten ++
          schema.items ++
          schema.contains ++
          schema.unevaluatedItems ++
          schema.properties.values ++
          schema.patternProperties.values ++
          schema.additionalProperties ++
          schema.propertyNames ++
          schema.unevaluatedProperties

      schema :: children.toList.flatMap(schemasIn)
    case AnySchema.Anything | AnySchema.Nothing => List.empty
  }

  private val schemas       = schemasIn(responseSchema)
  private val objectSchemas = schemas.filter(_.`type`.exists(_.contains(SchemaType.Object)))

  "AIClient response schema" should {
    "disallow additional properties in every object" in {
      objectSchemas.foreach { schema =>
        withClue(s"Object schema [${schema.title.getOrElse("anonymous")}] was open: ") {
          schema.additionalProperties shouldBe Some(AnySchema.Nothing)
        }
      }
    }

    "require every declared property, including nullable options" in {
      objectSchemas.foreach { schema =>
        withClue(s"Object schema [${schema.title.getOrElse("anonymous")}] had optional properties: ") {
          schema.required shouldBe schema.properties.keys.toList
        }
      }
    }

    "omit the general JSON Schema dialect declaration" in {
      responseSchema.$schema shouldBe None
    }

    "keep optional response fields nullable" in {
      val unidentifiedEntriesSummary =
        responseSchema.properties("unidentifiedEntriesSummary").asInstanceOf[ApiSchema]

      unidentifiedEntriesSummary.`type`.value should contain(SchemaType.Null)
    }

    "retain supported Iron string constraints" in {
      schemas.exists(_.pattern.nonEmpty) shouldBe true
      schemas.exists(_.maxLength.contains(255)) shouldBe true
    }

    "use the Smithy two-field phone shape for extracted candidates" in {
      objectSchemas.exists(_.properties.keySet == Set("phoneNationalNumber", "phoneCountryCode")) shouldBe true
      objectSchemas.exists(_.properties.contains("phoneRegion")) shouldBe false
      objectSchemas.exists(_.properties.contains("phoneNumberE164")) shouldBe false
    }
  }
}
