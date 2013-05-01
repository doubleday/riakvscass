package net.wooga.loadclient.riak

import com.basho.riak.client.RiakFactory

object Test extends App {

  val myPbClient = RiakFactory.pbcClient("127.0.0.1", 8087)

  val bucket = myPbClient.fetchBucket("TestBucket").execute()
  val result = Option(bucket.fetch("Name").execute())
  val value = result.map { _.getValueAsString } getOrElse null

  println("Read from riak: " + value)

  bucket.store("Name", "Daniel Doubleday").execute()
  myPbClient.shutdown()

}
