package io.rikkos.testkit.base

import io.github.iltotore.iron.*
import io.rikkos.domain.*
import org.scalacheck.*

trait DomainArbitraries extends IronRefinedTypeConversion {

  given Arbitrary[String :| NonEmptyTrimmedLowerCase] = Arbitrary {
    Gen
      .nonEmptyStringOf(Gen.alphaNumChar)
      .map(_.trim.toLowerCase)
      .map(_.refineUnsafe[NonEmptyTrimmedLowerCase])
  }

  given Arbitrary[AuthMember] = Arbitrary(Gen.resultOf(AuthMember.apply))
}
