name := """football_club"""
organization := "com.football"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.18"

libraryDependencies ++= Seq(
  guice,
  jdbc,
  evolutions,
  "mysql"        %  "mysql-connector-java" % "8.0.33",
  "io.getquill"  %% "quill-jdbc"           % "4.8.0",
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test
)



// Adds additional packages into Twirl
//TwirlKeys.templateImports += "com.football.controllers._"

// Adds additional packages into conf/routes
// play.sbt.routes.RoutesKeys.routesImport += "com.football.binders._"
