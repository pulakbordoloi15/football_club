name := """football_club"""
organization := "com.football"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.18"

libraryDependencies ++= Seq(
  guice,
  jdbc,
  evolutions,
  "mysql"                     %  "mysql-connector-java"      % "8.0.33",
  "io.getquill"              %% "quill-jdbc"                % "4.8.0",
  "org.scalatestplus.play"   %% "scalatestplus-play"        % "7.0.2" % Test
)

// ─── Docker packaging ───────────────────────────────────────────────
// `sbt docker:publishLocal` builds an image using sbt-native-packager
// (comes with the PlayScala plugin). The image name is the project name.
import com.typesafe.sbt.packager.docker._

// ─── Docker packaging (WIP — not fully working yet) ─────────────────
// Base plumbing for `sbt docker:publishLocal`. Compose file + app runtime
// need more debugging (connection refused inside the compose network).
// Keeping the config here so we can return to it later.
dockerBaseImage       := "eclipse-temurin:17-jre"
dockerExposedPorts    := Seq(9000)
dockerUpdateLatest    := true
Docker / packageName  := "football-club"

Universal / javaOptions ++= Seq(
  "-Dpidfile.path=/dev/null"
)
