/*
 * Copyright (c) 2016 SnappyData, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

package io.snappydata.adanalytics

import io.snappydata.adanalytics.Configs._
import kafka.serializer.StringDecoder
import org.apache.spark.sql.Row
import org.apache.spark.sql.streaming.SchemaDStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Duration, SnappyStreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

/**
 * Example using Spark API + Snappy extension to model a Stream as a DataFrame.
 * The Spark driver and executors run in local mode and simply use Snappy
 * cluster as the data store.
 */
object SnappyAPILogAggregator extends App {

  val conf = new SparkConf()
    .setAppName(getClass.getSimpleName)
    .setMaster(s"$sparkLocalURL") // local split
    //.set("snappydata.store.locators", s"$snappyLocators") // 0.6.1
    .set("spark.snappydata.connection", s"$snappyLocatorJDBC") // 0.9
    .set("spark.ui.port", "4042")
    .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
    .registerAvroSchemas(AdImpressionLog.getClassSchema)

  // add the "assembly" jar to executor classpath
  val assemblyJar = System.getenv("PROJECT_ASSEMBLY_JAR")
  if (assemblyJar != null) {
    conf.set("spark.driver.extraClassPath", assemblyJar)
    conf.set("spark.executor.extraClassPath", assemblyJar)
  }

  val sc = new SparkContext(conf)
  val ssc = new SnappyStreamingContext(sc, batchDuration)

  // The volumes are low. Optimize Spark shuffle by reducing the partition count
  ssc.sql("set spark.sql.shuffle.partitions=8")

  // stream of (topic, ImpressionLog)
  val messages = KafkaUtils.createDirectStream
    [String, AdImpressionLog, StringDecoder, AdImpressionLogAvroDecoder](ssc, kafkaParams, topics)

  // Filter out bad messages ...use a second window
  val logs = messages.map(_._2).filter(_.getGeo != Configs.UnknownGeo)
    .window(Duration(10000), Duration(10000))

  // We want to process the stream as a DataFrame/Table ... easy to run
  // analytics on stream ...will be standard part of Spark 2.0 (Structured streaming)
  val rows = logs.map(v => Row(
    new java.sql.Timestamp(v.getTimestamp), v.getPublisher.toString, v.getAdvertiser.toString,
    v.getWebsite.toString, v.getGeo.toString, v.getBid, v.getCookie.toString))

  val logStreamAsTable : SchemaDStream = ssc.createSchemaDStream(rows, getAdImpressionSchema)

  import org.apache.spark.sql.functions._

  /**
    * We want to execute the following analytic query ... using the DataFrame
    * API ...
    * select publisher, geo, avg(bid) as avg_bid, count(*) imps, count(distinct(cookie)) uniques
    * from AdImpressionLog group by publisher, geo, timestamp"
    */
  logStreamAsTable.foreachDataFrame(df => {
    val df1 = df.groupBy("publisher", "geo", "timestamp")
      .agg(avg("bid").alias("avg_bid"), count("geo").alias("imps"),
        countDistinct("cookie").alias("uniques"))
    df1.show()
  })

  // start rolling!
  ssc.start
  ssc.awaitTermination
}
