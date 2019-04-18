name := "akka-quickstart-scala"

version := "1.0"

scalaVersion := "2.12.6"

lazy val akkaVersion = "2.5.22"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster" % "2.5.22",
  "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % "1.0.0",
  "com.typesafe.akka" %% "akka-discovery" % "2.5.21",
  "org.scalatest" %% "scalatest" % "3.0.5" % "test"
)
