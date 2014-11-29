import sbt._
import sbt.Keys._
import xerial.sbt.Pack._

object Build extends sbt.Build {

  lazy val root = Project(
    id = "finagle-mqtt",
    base = file("."),
    settings = Defaults.defaultSettings
      ++ packSettings // This settings add pack and pack-archive commands to sbt
      ++ Seq(
      name := "finagle-mqtt",
      version := "0.0.1-SNAPSHOT",
      crossScalaVersions := Seq("2.9.2", "2.10.0"),

      resolvers += "Eclipse Paho Release" at "https://repo.eclipse.org/content/repositories/paho-releases/",
      resolvers += "OSChina" at "http://maven.oschina.net/content/groups/public/",
      resolvers += "twitter.com" at "http://maven.twttr.com/",
      libraryDependencies ++= Seq(
        "org.scala-lang" %% "scala-actors" % "2.10.0",
        "redis.clients" % "jedis" % "2.2.1",
        "org.redisson" % "redisson" % "1.0.2",
        "org.slf4s" %% "slf4s-api" % "1.7.6",
        "ch.qos.logback" % "logback-classic" % "1.1.1",
        "com.typesafe" % "config" % "1.2.0",
        "org.eclipse.paho" % "mqtt-client" % "0.4.0",
        "com.twitter" %% "finagle-core" % "6.13.0"
      )
    )
  )
}
