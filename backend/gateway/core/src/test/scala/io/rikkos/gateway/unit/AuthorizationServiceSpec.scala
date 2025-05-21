package io.rikkos.gateway.unit

import io.rikkos.gateway.auth.AuthorizationService
import io.rikkos.testkit.base.ZWordSpecBase
import zio.*

class AuthorizationServiceSpec extends ZWordSpecBase {

  "AuthorizationService" when {
    "authorize" should {
      "return a successful response" in {
        val authorizationService = ZIO.service[AuthorizationService].provide(AuthorizationService.live)

        final case class MemberDetails(
            id: String,
            email: String,
            age: Int,
            isVerified: Boolean,
        )

        given Decoder[Int] = ???
        given Decoder[String] = ???
        given Decoder[UUID] = ???
        given Decoder[Boolean] = ???

//        def generateDecoder[A](a: A)(given Decoder[(A)]): Decoder[A] = ???

        val decode: Decoder[MemberDetails] = generateDecoder[MemberDetails]

        def parseRequestEntity[A](dsdsd: Int= 1)(using Decoder[A])

        parseRequestEntity[MemberDetails]()

        given Int = 1

        def test(x: Int, multipler: Int): Int = x * multipler

        def test1(x: Int)(using multipler: Int): Int = x * multipler

        def test2(x: Int)(using multipler: Int): Int = x * multipler

        def test3(x: Int)(using multipler: Int): Int = x * multipler

        test(1, 1)
        test1(2)
        test2(2)
        test3(2)
      }
    }
  }
}
