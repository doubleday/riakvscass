package net.wooga.loadclient

import scala.util.{Success, Try, Random}
import com.basho.riak.client.RiakFactory
import me.prettyprint.hector.api.exceptions.HectorException
import me.prettyprint.hector.api.beans.HColumn
import me.prettyprint.hector.api.query.{ColumnQuery, QueryResult}
import me.prettyprint.hector.api.factory.HFactory
import me.prettyprint.cassandra.serializers.StringSerializer
import me.prettyprint.hector.api.mutation.Mutator
import me.prettyprint.hector.api.{Cluster, Keyspace}
import me.prettyprint.cassandra.service.{CassandraHost, ThriftCluster, FailoverPolicy, CassandraHostConfigurator}

trait DbConnection {
  def hostName: String

  def shutdown()

  def read(key: String): Try[String]

  def write(key: String, value: String): Try[Any]
}

class Riak(val hostName: String) extends DbConnection {

  val bucketName = Config.riakBucket
  val myPbClient = RiakFactory.pbcClient(hostName, 8087)

  lazy val bucket = myPbClient.fetchBucket(bucketName).execute()

  def read(key: String): Try[String] = {
    Try(Option(bucket.fetch(key).execute()).map {
      _.getValueAsString
    } getOrElse null)
  }

  def write(key: String, value: String) = {
    Try(bucket.store(key, value).withoutFetch().execute())
  }

  def shutdown() = myPbClient.shutdown()
}

class Cassandra(val hostName: String) extends DbConnection {
//  val hosts = Config.cassandraServers.map( _ + ":9160" ).mkString(",")
//  val cluster = HFactory.getOrCreateCluster("Test", new CassandraHostConfigurator(hosts))
  val cluster = {
    val configurator = new CassandraHostConfigurator(hostName + ":9160")
    configurator.setMaxActive(1)
    new ThriftCluster("Test", configurator, null)
  }

  val keyspaceOperator = HFactory.createKeyspace(Config.cassandraKeyspace, cluster, HFactory.createDefaultConsistencyLevelPolicy(), FailoverPolicy.FAIL_FAST)

  def read(key: String): Try[String] = {
    val columnQuery = HFactory.createStringColumnQuery(keyspaceOperator)
    columnQuery.setColumnFamily(Config.cassandraColumnFamily).setKey(key).setName("0")
    Try(Option(columnQuery.execute().get()).map {
      _.getValue
    } getOrElse null)
  }

  def write(key: String, value: String) = {
    val mutator: Mutator[String] = HFactory.createMutator(keyspaceOperator, StringSerializer.get())
    Try(mutator.insert(key, Config.cassandraColumnFamily, HFactory.createStringColumn("0", value)))
  }

  def shutdown() = HFactory.shutdownCluster(cluster)
}

object DbConnection {

  type Factory = (String) => Option[DbConnection]

  def riak: Factory = (host: String) => {
    val riak = new Riak(host)
    riak.read("test") match {
      case Success(_) => Some(riak)
      case _ => None
    }
  }

  def cassandra: Factory = (host: String) => {
    val cassandra = new Cassandra(host)
    cassandra.read("test") match {
      case Success(_) => Some(cassandra)
      case _ => None
    }
  }

}
