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

import com.miguno.kafka.avro.{AvroEncoder, AvroDecoder}
import kafka.utils.VerifiableProperties
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.Row
import org.apache.spark.sql.streaming.{StreamConverter, StreamToRowsConverter}

class AdImpressionLogAvroDecoder(props: VerifiableProperties = null)
  extends AvroDecoder[AdImpressionLog](props, AdImpressionLog.getClassSchema)

class AdImpressionLogAvroEncoder(props: VerifiableProperties = null)
  extends AvroEncoder[AdImpressionLog](props, AdImpressionLog.getClassSchema)

class AdImpressionToRowsConverter extends StreamToRowsConverter with Serializable {

  override def toRows(message: Any): Seq[Row] = {
    val log = message.asInstanceOf[AdImpressionLog]
    Seq(Row.fromSeq(Seq(
      new java.sql.Timestamp(log.getTimestamp),
      log.getPublisher.toString,
      log.getAdvertiser.toString,
      log.getWebsite.toString,
      log.getGeo.toString,
      log.getBid,
      log.getCookie.toString)))
  }
}

// Convertes Spark RDD[AdImpressionLog] to RDD[Row] to insert into table
class AdImpressionLogToRowRDD extends Serializable {

  def convert(logRdd: RDD[AdImpressionLog]): RDD[Row] = {
    logRdd.map(log => {
      Row(log.getTimestamp,
        log.getPublisher.toString,
        log.getAdvertiser.toString,
        log.getWebsite.toString,
        log.getGeo.toString,
        log.getBid,
        log.getCookie.toString)
    })
  }
}

class AvroSocketStreamConverter extends StreamConverter with Serializable {
  override def convert(inputStream: java.io.InputStream): Iterator[AdImpressionLog] = {
    val reader = new SpecificDatumReader[AdImpressionLog](AdImpressionLog.getClassSchema)
    val decoder = DecoderFactory.get().directBinaryDecoder(inputStream, null)
    new Iterator[AdImpressionLog] {

      val log: AdImpressionLog = new AdImpressionLog()
      var nextVal = log
      nextVal = reader.read(nextVal, decoder)

      override def hasNext: Boolean = nextVal != null

      override def next(): AdImpressionLog = {
        val n = nextVal
        if (n ne null) {
          nextVal = reader.read(nextVal, decoder)
          n
        } else {
          throw new NoSuchElementException()
        }
      }
    }
  }

  override def getTargetType: scala.Predef.Class[_] = classOf[AdImpressionLog]
}
