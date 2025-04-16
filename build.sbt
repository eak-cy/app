Global / onChangedBuildSource := ReloadOnSourceChanges

ThisBuild / scalaVersion := "3.3.5"
ThisBuild / version := "local"
ThisBuild / organization := "io.farmer.app"
ThisBuild / organizationName := "Farmers App"

lazy val root = Project("app", file("."))
  .aggregate(backendModule)

lazy val backendModule = Project("backend", file("backend"))
  .aggregate(backendGatewayModule, backendDomainModule)

lazy val backendGatewayModule = Project("backend-gateway", file("backend/gateway"))

lazy val backendDomainModule = Project("backend-domain", file("backend/domain"))

lazy val modules: Seq[ProjectReference] = Seq(
  backendModule,
)