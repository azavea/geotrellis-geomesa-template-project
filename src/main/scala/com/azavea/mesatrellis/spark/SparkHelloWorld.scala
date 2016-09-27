package com.azavea.mesatrellis.spark

import org.apache.spark._

// Spark hello world example
object SparkHelloWorld {
  def helloSentence = "Hello GeoTrellis"

  def main(args: Array[String]): Unit = {

    // Initialise spark context
    val conf = new SparkConf().setAppName("HelloWorld")
    implicit val sc = new SparkContext(conf)

    // Making and RDD from a char array
    val rdd = sc.makeRDD(helloSentence.toCharArray)

    // Shift every char +1 to the right, collect data on a driver and making a new string
    val sentence = rdd.map(c => c + 1).collect().mkString

    // 73102109109112337210211285115102109109106116
    println(sentence)

    // Stop Spark context
    sc.stop()
  }
}
