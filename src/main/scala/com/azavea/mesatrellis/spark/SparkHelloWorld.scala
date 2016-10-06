package com.azavea.mesatrellis.spark

import org.apache.spark._

// Spark hello world example
object SparkHelloWorld {
  def helloSentence = "Hello GeoTrellis"

  def main(args: Array[String]): Unit = {

    // Initialise spark context
    // config definition to run app via spark-submit
    val conf = new SparkConf().setAppName("HelloWorld")
    // config definition to run spark app through sbt / IDE
    // if you want to launch this app via sbt run or from IDE don't forget to set Spark master to local / hostname
    // just replace code line above by the line below 
    // val conf = new SparkConf().setAppName("HelloWorld").setMaster("local[*]") // or spark://spark-master:7077
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
