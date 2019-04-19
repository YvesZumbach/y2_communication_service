name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.12.6"

lazy val akkaVersion = "2.5.22"

libraryDependencies ++= Seq(
  "com.github.scopt" %% "scopt" % "3.5.0",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-simple" % "1.7.5",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % "2.5.22",
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % "1.0.0",
  "com.typesafe.akka" %% "akka-discovery" % "2.5.22",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)
