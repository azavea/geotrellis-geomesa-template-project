package com.azavea.mesatrellis.feature

import com.azavea.mesatrellis.geotools.GeoMesaSimpleFeatureType
import geotrellis.geomesa.geotools._
import geotrellis.spark.LayerId
import geotrellis.vector._
import geotrellis.spark.io.geomesa._
import geotrellis.spark.io.kryo.KryoRegistrator

import org.apache.spark.rdd.RDD
import org.geotools.data.Query
import org.geotools.filter.text.ecql.ECQL
import org.apache.spark._
import org.apache.spark.serializer.KryoSerializer

import java.text.SimpleDateFormat
import java.util.TimeZone

import scala.io.StdIn

/**
  * GeoMesa Query synthetic example,
  * Generates SimpleFeatures, ingests and queries
  *
  * We can divide this job into steps:
  *  1. Features generation
  *  2. Ingest into Accumulo
  *  3. Query example
  */
object GeoMesaQuery {
  val sdf = {
    val df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    df.setTimeZone(TimeZone.getTimeZone("UTC")); df
  }

  val dates = (1 to 100).map { x =>
    val day = { val i = x / 10; if(i == 0) "01" else if (i < 10) s"0$i" else s"$i" }
    day.toInt -> sdf.parse(s"2010-05-${day}T00:00:00.000Z")
  }

  // 1. Array of GeoTrellis Feature[G, D] generation
  val featuresTemporal: Array[Feature[Point, Map[String, Any]]] =
    (1 to 100).zip(dates).map { case (x, (day, strDay)) =>
      Feature(Point(x, 40), Map[String, Any](GeometryToGeoMesaSimpleFeature.whenField -> strDay)) }.toArray

  val spaceTimeFeatureName = "spaceTimeFeature"
  val spaceTimeFeatureLayerId = LayerId(spaceTimeFeatureName, 0)
  val spaceTimeFeatureType = GeoMesaSimpleFeatureType[Point](spaceTimeFeatureName, temporal = true)

  def main(args: Array[String]): Unit = {

    // Setup Spark to use Kryo serializer.
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("Spark GeoMesa Query")
      .set("spark.serializer", classOf[KryoSerializer].getName)
      .set("spark.kryo.registrator", classOf[KryoRegistrator].getName)

    implicit val sc = new SparkContext(conf)

    try {
      run
      // Pause to wait to close the spark context,
      // so that you can check out the UI at http://localhost:4040
      println("Hit enter to exit.")
      StdIn.readLine()
    } finally {
      sc.stop()
    }
  }

  def fullPath(path: String) = new java.io.File(path).getAbsolutePath

  def run(implicit sc: SparkContext) = {
    // Accumulo connection settings, note that ingest table needs to be specified there
    // it works with "featuresTemporal" table
    val featuresTemporalInstance = GeoMesaInstance(
      tableName    = "featuresTemporal",
      instanceName = "gis",
      zookeepers   = "localhost",
      user         = "root",
      password     = "secret"
    )

    // Feature layer writer and reader
    val layerWriter = new GeoMesaFeatureWriter(featuresTemporalInstance)
    val layerReader = new GeoMesaFeatureReader(featuresTemporalInstance)

    // make an RDD from Array of Feature[G, D]
    val featuresTemporalRDD: RDD[Feature[Point, Map[String, Any]]] = sc.parallelize(featuresTemporal)


    // 2. RDD Features Ingest
    layerWriter.write(spaceTimeFeatureLayerId, spaceTimeFeatureType, featuresTemporalRDD)

    // 3. Query
    // Dates filter (to test query result)
    val ds = dates.filter { case (k, _) => k > 3 && k < 6 }
    // Expected Features lenght
    val expectedLength = ds.length
    // Create GeoMesa query
    val filter = ECQL.toFilter(s"${GeometryToGeoMesaSimpleFeature.whenField} between '${sdf.format(ds.head._2)}' and '${sdf.format(ds.last._2)}'")

    // Query data and collect it
    val actual =
      layerReader
        .read[Point, Map[String, Any]](spaceTimeFeatureLayerId, spaceTimeFeatureType, new Query(spaceTimeFeatureName, filter))
        .collect()

    println(s"actual.length: ${actual.length}; expectedLength: ${expectedLength}")
    println("actual:")
    actual.foreach(println)
  }
}

