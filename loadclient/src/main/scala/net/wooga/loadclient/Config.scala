package net.wooga.loadclient

import scala.io.Source
import play.api.libs.json.Json

object Config {

  private val config = System.getProperty("TEST_CONFIG") match {
    case testConfig:String => Json.parse(Source.fromFile(testConfig).mkString)
    case _ => throw new IllegalStateException("TEST_CONFIG not set")
  }

  lazy val graphite    = (config\"graphite").as[Boolean]

  lazy val riakServers = (config\"riak"\"servers").as[List[String]]
  lazy val riakBucket  = (config\"riak"\"bucket").as[String]

  lazy val cassandraServers      = (config\"cassandra"\"servers").as[List[String]]
  lazy val cassandraKeyspace     = (config\"cassandra"\"keyspace").as[String]
  lazy val cassandraColumnFamily = (config\"cassandra"\"columnFamily").as[String]

}