package net.wooga.loadclient
import scala.util.{Success, Try, Random}
import com.basho.riak.client.RiakFactory

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
    Try(Option(bucket.fetch(key).execute()).map { _.getValueAsString } getOrElse null)
  }

  def write(key: String, value: String) = {
    Try(bucket.store(key, value).withoutFetch().execute())
  }

  def shutdown() = myPbClient.shutdown()
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

  def cassandra: Factory = (host: String) => ???

}
