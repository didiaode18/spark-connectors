package io.snappydata.benchmark

import java.util.Properties

import io.snappydata.adanalytics.Configs._
import io.snappydata.adanalytics.KafkaAdImpressionProducer._
import io.snappydata.adanalytics.{AdImpressionGenerator, AdImpressionLog, Configs}
import kafka.producer.{KeyedMessage, Producer, ProducerConfig}

/**
  * A simple Kafka Producer program which randomly generates
  * ad impression log messages and sends it to Kafka broker.
  * This program generates and sends 10 million messages.
  */
object KafkaAdImpressionAsyncProducer{

  val props = new Properties()
  props.put("producer.type", "async")
  props.put("request.required.acks", "0")
  props.put("serializer.class", "io.snappydata.adanalytics.AdImpressionLogAvroEncoder")
  props.put("queue.buffering.max.messages", "1000000") // 10000
  props.put("metadata.broker.list", brokerList)
  props.put("partitioner.class", "kafka.producer.DefaultPartitioner")
  props.put("key.serializer.class", "kafka.serializer.StringEncoder")
  props.put("batch.size", "9000000") // bytes
  props.put("linger.ms", "50")

  val config = new ProducerConfig(props)
  val producer = new Producer[String, AdImpressionLog](config)

  def main(args: Array[String]) {
    println("Sending Kafka messages of topic " + kafkaTopic + " to brokers " + brokerList)
    val threads = new Array[Thread](numProducerThreads)
    for (i <- 0 until numProducerThreads) {
      val thread = new Thread(new Worker())
      thread.start()
      threads(i) = thread
    }
    threads.foreach(_.join())
    println(s"Done sending $numLogsPerThread Kafka messages of topic $kafkaTopic")
    System.exit(0)
  }

  def sendToKafka(log: AdImpressionLog) = {
    producer.send(new KeyedMessage[String, AdImpressionLog](
      Configs.kafkaTopic, log.getTimestamp.toString, log))
  }
}

final class Worker extends Runnable {
  def run() {
    for (j <- 0 to numLogsPerThread by maxLogsPerSecPerThread) {
      val start = System.currentTimeMillis()
      for (i <- 0 to maxLogsPerSecPerThread) {
        sendToKafka(AdImpressionGenerator.nextRandomAdImpression())
      }
      // If one second hasn't elapsed wait for the remaining time before queueing more.
      val timeRemaining = 1000 - (System.currentTimeMillis() - start)
      if (timeRemaining > 0) {
        Thread.sleep(timeRemaining)
      }
      if (j !=0 & (j % 200000) == 0) {
        println(s"Sent $j Kafka messages of topic $kafkaTopic")
      }
    }
  }
}
