import sbt._
import sbt.Keys._

object FinagleMqtt extends Build {


  lazy val basicSettings = Seq(
    organization := "com.yiguang.mqtt",
    version := "0.0.2-SNAPSHOT",
    scalaVersion := "2.10.4",
    //finagle not support scala 2.11
    //crossScalaVersions := Seq("2.10.4", "2.11.4"),
    resolvers ++= Seq(
      "OSChina" at "http://maven.oschina.net/content/groups/public/",
      "twitter.com" at "http://maven.twttr.com/"
    )
  )

  lazy val finagleMqtt = Project("finagle-mqtt",file("."))
    .settings(basicSettings:_*)
    .settings(libraryDependencies ++= Dependencies.all)

  object Dependencies {
    val jedis         = "redis.clients" % "jedis" % "2.2.1"
    val akka_actor    = "com.typesafe.akka" %% "akka-actor" % "2.3.7"
    val logback       = "ch.qos.logback" % "logback-classic" % "1.1.1"
    val slf4s         = "org.slf4s" %% "slf4s-api" % "1.7.6"
    val finagle_core  = "com.twitter" %% "finagle-core" % "6.22.0"
    val netty_common  = "io.netty" % "netty-common" % "4.0.24.Final"
    val scala_test    = "org.scalatest" %% "scalatest" % "2.2.1"

    val all = Seq(jedis,akka_actor,logback,slf4s,finagle_core,netty_common,scala_test)
  }
}
