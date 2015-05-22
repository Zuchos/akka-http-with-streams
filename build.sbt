lazy val commonSettings = Seq(
  organization := "pl.zuchos",
  version := "0.0.1-SNAPSHOT",
  scalaVersion := "2.11.6"
) ++ Revolver.settings

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    name := "akka-http-with-streams",
    libraryDependencies ++= Dependencies.backendDependencies
  )
