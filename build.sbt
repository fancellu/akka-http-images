name := "akka-http-images"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http"   % "10.1.8",
  "com.typesafe.akka" %% "akka-stream" % "2.5.19",

  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.19" % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % "10.1.8" % Test,
  "org.scalactic" %% "scalactic" % "3.0.8",
  "org.scalatest" %% "scalatest" % "3.0.8" % Test
)