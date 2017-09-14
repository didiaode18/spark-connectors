package io.snappydata.benchmark

import java.io.PrintWriter

import com.typesafe.config.Config
import org.apache.spark.sql._

class SnappySampleQueryPerfJob extends SnappySQLJob {

  override def runSnappyJob(sc: SnappySession, jobConfig: Config): Any = {
    val outFileName = s"SampleQueryPerf-${System.currentTimeMillis()}.out"
    val pw = new PrintWriter(outFileName)
    var start = System.currentTimeMillis()
    sc.sql("select count(*) AS adCount, geo from adImpressions group by geo" +
      " order by adCount desc limit 20 with error 0.1").collect()
    pw.println("Time for Sample Q1 " + (System.currentTimeMillis() - start))
    pw.flush()

    start = System.currentTimeMillis()
    sc.sql("select sum (bid) as max_bid, geo from adImpressions group by geo" +
      " order by max_bid desc limit 20 with error 0.1").collect()
    pw.println("Time for Sample Q2 " + (System.currentTimeMillis() - start))
    pw.flush()

    start = System.currentTimeMillis()
    val array = sc.sql("select count(*) as sample_cnt from" +
      " adImpressions with error 0.1").collect()
    pw.println(array(0) +"Time for sample count(*) " + (System.currentTimeMillis() - start))
    pw.flush()

    start = System.currentTimeMillis()
    sc.sql("select sum (bid) as max_bid, publisher from adImpressions group by" +
      " publisher order by max_bid desc limit 20 with error 0.5").collect()
    pw.println("Time for Sample Q3 " + (System.currentTimeMillis() - start))
    pw.flush()

    pw.close()
  }

  override def isValidJob(sc: SnappySession, config: Config): SnappyJobValidation = {
    SnappyJobValid()
  }
}
