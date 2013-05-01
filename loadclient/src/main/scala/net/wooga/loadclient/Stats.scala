package net.wooga.loadclient

import com.yammer.metrics.reporting.ConsoleReporter
import java.util.concurrent.TimeUnit
import com.yammer.metrics.Metrics
import java.net.InetAddress
import com.yammer.metrics.core.{Histogram, MetricName}

object Stats {

  ConsoleReporter.enable(5, TimeUnit.SECONDS)

  val hostName = InetAddress.getLocalHost.getHostName

  lazy val readCounter = counter("read")
  lazy val requestCounter = counter("request")
  lazy val readHisto: Histogram = Metrics.defaultRegistry().newHistogram(metricName("readHisto"), true)

  def incCounter(name: String)(value: Int = 1) = counter(name).inc(value)

  def counter(name: String) = Metrics.defaultRegistry().newCounter(metricName(name))

  def metricName(name: String) = new MetricName(hostName, "loadclient", name)
}
