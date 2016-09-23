package com.azavea.mesatrellis.raster

import com.typesafe.scalalogging.LazyLogging
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.render._

object CreateNDVIPng extends LazyLogging {
  val maskedPath = "data/rgb-nir.tif"
  val ndviPath = "data/ndvi.png"
  val colorRamp = "0:ffffe5ff;0.1:f7fcb9ff;0.2:d9f0a3ff;0.3:addd8eff;0.4:78c679ff;0.5:41ab5dff;0.6:238443ff;0.7:006837ff;1:004529ff"

  def main(args: Array[String]): Unit = {
    val ndvi = {
      // Convert the tile to type double values,
      // because we will be performing an operation that
      // produces floating point values.
      logger.info("Reading in multiband image...")
      val tile = MultibandGeoTiff(maskedPath).convert(DoubleConstantNoDataCellType)

      // Use the combineDouble method to map over the red and infrared values
      // and perform the NDVI calculation.
      logger.info("Performing NDVI calculation...")
      tile.combineDouble(0, 3) { (r: Double, ir: Double) =>
        if(isData(r) && isData(ir)) {
          (ir - r) / (ir + r)
        } else {
          Double.NaN
        }
      }
    }

    // Get color map from the application.conf settings file.
    val colorMap = ColorMap.fromStringDouble(colorRamp).get

    // Render this NDVI using the color breaks as a PNG,
    // and write the PNG to disk.
    logger.info("Rendering PNG and saving to disk...")
    ndvi.renderPng(colorMap).write(ndviPath)
  }
}
