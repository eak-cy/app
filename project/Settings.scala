import org.typelevel.sbt.tpolecat.TpolecatPlugin.autoImport.tpolecatScalacOptions
import org.typelevel.scalacoptions.ScalacOptions
import sbt.*

object Settings {

  /** Ignore scalatest unused value Assertion for multi-line assertion in asserting
    *
    * @example
    *   {{{
    *   .asserting { entities =>
    *     entities should have size numOfUsers // <- WARN: unused value of type org.scalatest.Assertion
    *     entities.forall(_.ids.size == 30) shouldBe true
    *   }
    *   }}}
    */
  private val ignoreNotUsedAssertion =
    ScalacOptions.other("-Wconf:msg=unused value of type:s")

  lazy val ScalaCompiler = Def.settings(
    tpolecatScalacOptions ++= Set(
      ScalacOptions.other("-no-indent"),
      ScalacOptions.other("-experimental"),
      ScalacOptions.other("-language:experimental.betterFors"),
      ScalacOptions.other("-language:experimental.namedTuples"),
      ScalacOptions.other("-old-syntax"),
      ScalacOptions.other("-Wunused:unsafe-warn-patvars"),
      ScalacOptions.other("-Wunused:all"),
    ),
    Test / tpolecatScalacOptions ++= Set(
      ignoreNotUsedAssertion
    ),
  )
}
