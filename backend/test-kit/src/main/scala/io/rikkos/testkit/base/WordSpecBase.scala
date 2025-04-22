package io.rikkos.testkit.base

import org.scalatest.concurrent.Eventually
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.EitherValues
import org.scalatest.LoneElement
import org.scalatest.OptionValues
import scala.concurrent.duration.DurationInt

open class WordSpecBase
    extends AnyWordSpec
    with PropertyBase
    with should.Matchers
    with OptionValues
    with EitherValues
    with Eventually
    with ScalaFutures
    with LoneElement {

  implicit override val patienceConfig: PatienceConfig = PatienceConfig(10.seconds, 200.millis)
}
