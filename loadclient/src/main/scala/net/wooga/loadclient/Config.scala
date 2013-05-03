package net.wooga.loadclient

import scala.io.Source
import play.api.libs.json.Json

object Config {

  private val config = System.getProperty("TEST_CONFIG") match {
    case testConfig:String => Json.parse(Source.fromFile(testConfig).mkString)
    case _ => throw new IllegalStateException("TEST_CONFIG not set")
  }

  lazy val riakServers = (config\"riak"\"servers").as[List[String]]
  lazy val riakBucket  = (config\"riak"\"bucket").as[String]
  lazy val numServers  = (config\"numServers").as[Int]
  lazy val graphite    = (config\"graphite").as[Boolean]

}