# GeoTrellis GeoMesa Template (MesaTrellis)

[![Join the chat at https://gitter.im/geotrellis/geotrellis](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/geotrellis/geotrellis?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Libs / Supported environment

* Java 8
* Scala 2.11.8
* [GeoTrellis](https://github.com/geotrellis/geotrellis) `1.0.0-SNAPSHOT`
* [GeoMesa](https://github.com/locationtech/geomesa/) `1.2.6`
* [GeoDocker cluster](https://github.com/geotrellis/geodocker-cluster) `latest`
* [Spark](http://spark.apache.org/) `2.0`
* [Hadoop](http://hadoop.apache.org/) `2.7.3`
* [Accumulo](http://accumulo.apache.org/) `1.7.2`

## Tutorial description (each example has additional informtion in comments)

* [HelloWorld.scala](src/main/scala/com/azavea/mesatrellis/HelloWorld.scala)
  * A common scala HelloWorld
* [SparkHelloWorld.scala](src/main/scala/com/azavea/mesatrellis/spark/SparkHelloWorld.scala)
  * A Spark HelloWorld
* [MultibandLandsatIngest.scala](src/main/scala/com/azavea/mesatrellis/spark/MultibandLandsatIngest.scala)
  * An example of a hand written ingest job
* [CreateNDVIPng.scala](src/main/scala/com/azavea/mesatrellis/raster/CreateNDVIPng.scala)
  * An example of a hand written ingest job

## Building assembly

You can build demo with all examples:

```bash
./sbt assembly
```

Result fat jar is `target/scala-2.11/mesatrellis-assembly-0.1.0-SNAPSHOT.jar`

## [GeoDocker Cluster](https://github.com/geodocker/geodocker)

To compile and run this demo, we prepared an [environment](https://github.com/geodocker/geodocker). To run cluster we have a bit modified [docker-compose.yml](docker-compose.yml) file:

* To run cluster:
  ```bash
    docker-compose up
  ```

To check that cluster is operating normally check pages availability:
  * Hadoop [http://localhost:50070/](http://localhost:50070/)
  * Accumulo [http://localhost:50095/](http://localhost:50095/)
  * Spark [http://localhost:8080/](http://localhost:8080/)

To check containers status is possible using following command:

  ```bash
  docker ps -a | grep geodocker
  ```

More information avaible in a [GeoDocker cluster](https://github.com/geodocker/geodocker) repo.

## How to run examples

## License

* Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
