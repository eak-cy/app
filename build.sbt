Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion := "3.3.5"
ThisBuild / version := "local"
ThisBuild / organization := "io.farmer.app"
ThisBuild / organizationName := "Farmers App"

ThisBuild / scalafixOnCompile := !sys.env.getOrElse("DISABLE_SCALA_LINT_ON_COMPILE", "false").toBoolean
ThisBuild / scalafmtOnCompile := !sys.env.getOrElse("DISABLE_SCALA_LINT_ON_COMPILE", "false").toBoolean

lazy val root = Project("app", file("."))
  .aggregate(backendModule)
  .settings(Aliases.all)

lazy val backendModule = Project("backend", file("backend"))
  .aggregate(backendGatewayModule, backendDomainModule)

lazy val backendGatewayModule = Project("backend-gateway", file("backend/gateway"))

lazy val backendDomainModule = Project("backend-domain", file("backend/domain"))

lazy val modules: Seq[ProjectReference] = Seq(
  backendModule,
)
