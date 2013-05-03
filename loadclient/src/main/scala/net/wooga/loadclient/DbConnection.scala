package net.wooga.loadclient
import scala.util.{Try, Random}
import com.basho.riak.client.RiakFactory

trait DbConnection {
  implicit
  def shutdown()
  def read(key: String): Try[String]
  def write(key: String, value: String): Try[Any]
}

class Riak extends DbConnection {

  val bucketName = Config.riakBucket
  val myPbClient = RiakFactory.pbcClient(Random.shuffle(Config.riakServers).head, 8087)

  lazy val bucket = myPbClient.fetchBucket(bucketName).execute()

  def read(key: String): Try[String] = {
    Try(Option(bucket.fetch(key).execute()).map { _.getValueAsString } getOrElse null)
  }


  def write(key: String, value: String) = {
    Try(bucket.store(key, value).withoutFetch().execute())
  }


  def shutdown() = myPbClient.shutdown()
}

object DbConnection {

  type Factory = () => DbConnection

  def riak: Factory = () => new Riak

}
