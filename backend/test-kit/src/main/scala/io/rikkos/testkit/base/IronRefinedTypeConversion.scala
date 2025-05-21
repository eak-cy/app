package io.rikkos.testkit.base

import io.github.iltotore.iron.RefinedType
import org.scalacheck.Arbitrary

trait IronRefinedTypeConversion {

  given [WrappedType](using
      mirror: RefinedType.Mirror[WrappedType],
      arb: Arbitrary[mirror.IronType],
  ): Arbitrary[WrappedType] = arb.asInstanceOf[Arbitrary[WrappedType]]
}
