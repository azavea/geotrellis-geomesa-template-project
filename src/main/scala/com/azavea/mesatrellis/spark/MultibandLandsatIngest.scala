package com.azavea.mesatrellis.spark

import java.io.File

import geotrellis.proj4._
import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.hadoop._
import geotrellis.spark.io.index._
import geotrellis.spark.pyramid._
import geotrellis.spark.tiling._
import geotrellis.vector._
import org.apache.accumulo.core.client.security.tokens.PasswordToken
import org.apache.spark._
import org.apache.spark.rdd._

import scala.io.StdIn

/**
  * Manual Spatial Multiband job ingest example,
  * Otherwise it is possible to use ETL (we need to add an example)
  *
  * This job ingests inputPath tiff (if you want to ingest a bunch
  * of tiles it is possible to use directory of tiles instead of a certain tile)
  * into Apache Accumulo "tile" table, with pyramiding and reprojection to WebMercator
  * We can divide this job into steps:
  *  1. Load input rdd of (ProjectedExtend, MultibandTile) pairs
  *  2. Tiling input in tiles native projection
  *  3. Reprojection into WebMercator into ZoomedLayoutScheme
  *  4. Pyramiding up and writing everything into Accumulo
  */
object MultibandLandsatIngest {
  val inputPath  = "file://" + new File("data/landsat/rgb-nir.tif").getAbsolutePath

  def main(args: Array[String]): Unit = {

    // Setup Spark to use Kryo serializer.
    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("Spark LC Ingest")
      .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
      .set("spark.kryo.registrator", "geotrellis.spark.io.kryo.KryoRegistrator")

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
    // 1. Read the geotiff in as a single image RDD,
    // using a method implicitly added to SparkContext by
    // an implicit class available via the
    // "import geotrellis.spark.io.hadoop._ " statement.
    val inputRdd: RDD[(ProjectedExtent, MultibandTile)] =
    sc.hadoopMultibandGeoTiffRDD(inputPath)

    // Use the "TileLayerMetadata.fromRdd" call to find the zoom
    // level that the closest match to the resolution of our source image,
    // and derive information such as the full bounding box and data type.
    val (_, rasterMetaData) =
    TileLayerMetadata.fromRdd(inputRdd, FloatingLayoutScheme(512))

    // 2. Use the Tiler to cut our tiles into tiles that are index to a floating layout scheme.
    // We'll repartition it so that there are more partitions to work with, since spark
    // likes to work with more, smaller partitions (to a point) over few and large partitions.
    val tiled: RDD[(SpatialKey, MultibandTile)] =
    inputRdd
      .tileToLayout(rasterMetaData.cellType, rasterMetaData.layout, Bilinear)
      .repartition(100)
      .cache()

    // We'll be tiling the images using a zoomed layout scheme
    // in the web mercator format (which fits the slippy map tile specification).
    // We'll be creating 256 x 256 tiles.
    val layoutScheme = ZoomedLayoutScheme(WebMercator, tileSize = 256)

    // 3. We need to reproject the tiles to WebMercator
    val (zoom, reprojected): (Int, RDD[(SpatialKey, MultibandTile)] with Metadata[TileLayerMetadata[SpatialKey]]) =
    MultibandTileLayerRDD(tiled, rasterMetaData)
      .reproject(WebMercator, layoutScheme, Bilinear)

    // Create AccumuloInstance, to provide connection with Accumulo
    val instance = AccumuloInstance(
      instanceName = "gis",
      zookeeper    = "localhost",
      user         = "root",
      token        = new PasswordToken("secret")
    )

    // Create the writer that we will use to store the tiles in the Accumulo table "tiles".
    val writer = AccumuloLayerWriter(instance, table = "tiles")

    // 4. Pyramiding up the zoom levels, write our tiles out to the local file system.
    Pyramid.upLevels(reprojected, layoutScheme, zoom, Bilinear) { (rdd, z) =>
      val layerId = LayerId("landsat", z)
      writer.write(layerId, rdd, ZCurveKeyIndexMethod)
    }
  }
}
