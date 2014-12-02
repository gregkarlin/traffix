name := """Traffic Monitor"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(  
  "org.sorm-framework" % "sorm" % "0.3.15",
  "com.h2database" % "h2" % "1.4.177",
  "com.typesafe.akka" %% "akka-actor" % "2.3",
  "net.debasishg" %% "redisclient" % "2.13"
)

libraryDependencies += ws

