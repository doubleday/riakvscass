package net.wooga.loadclient

import com.yammer.metrics.reporting.{GraphiteReporter, ConsoleReporter}
import java.util.concurrent.TimeUnit
import com.yammer.metrics.Metrics
import java.net.InetAddress
import com.yammer.metrics.core.{Histogram, MetricName}

object Stats {

  if (Config.graphite) {
    GraphiteReporter.enable(10, TimeUnit.SECONDS, "graphite", 2003)

  } else {
    ConsoleReporter.enable(5, TimeUnit.SECONDS)
  }


  val hostName = InetAddress.getLocalHost.getHostName

  lazy val readCounter = counter("read")
  lazy val writeCounter = counter("write")
  lazy val errorCounter = counter("error")
  lazy val timeoutCounter = counter("timeout")
  lazy val notFoundCounter = counter("notfound")
  lazy val loginCounter = counter("login")

  lazy val readHisto: Histogram = histo("readHisto")
  lazy val writeHisto: Histogram = histo("writeHisto")

  def histo(name: String): Histogram = Metrics.defaultRegistry().newHistogram(metricName(name), true)
  def counter(name: String) = Metrics.defaultRegistry().newCounter(metricName(name))

  def metricName(name: String) = new MetricName(hostName, "loadclient", name)
}
