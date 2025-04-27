import sbt.addCommandAlias

object Aliases {

  lazy val all = scalaFmt ++ scalaFix ++ scalaLint ++ gatewayPrBuild

  lazy val scalaLint = addCommandAlias("checkLint", "clean; checkFix; checkFmt") ++
    addCommandAlias("runLint", "clean; runFmt; runFix")

  lazy val scalaFmt = addCommandAlias("checkFmt", "scalafmtCheckAll; scalafmtSbtCheck") ++
    addCommandAlias("runFmt", "scalafmtAll; scalafmtSbt")

  lazy val scalaFix = addCommandAlias("checkFix", "scalafixAll --check") ++
    addCommandAlias("runFix", "scalafixAll")

  lazy val gatewayPrBuild = addCommandAlias("gateway-pr-build", "clean; show scalafixOnCompile; checkLint; test")
}
