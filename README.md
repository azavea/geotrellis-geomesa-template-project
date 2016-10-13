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

Mounted volumes into Spark master continaer:

```bash
- '${PWD}/data/landsat:/data/landsat'
- '${PWD}/target/scala-2.11:/data/jars'
```

More information avaible in a [GeoDocker cluster](https://github.com/geodocker/geodocker) repo.

## How to run examples

* Log into Spark master container:
  ```bash
  docker exec -it geotrellisgeomesatemplateproject_spark-master_1 bash
  ```

* Run job:
  ```bash
  cd /
  CLASS_NAME=com.azavea.mesatrellis.spark.SparkHelloWorld
  spark-submit \
    --class ${CLASS_NAME} \
    --driver-memory=2G \
    ./data/jars/mesatrellis-assembly-0.1.0-SNAPSHOT.jar
  ```

  Class name can be any main class from the fat jar.

* Run a simple scala application (HelloWorld as an example):

  ```bash
  CLASS_NAME=com.azavea.mesatrellis.HelloWorld
  java -cp ./data/jars/mesatrellis-assembly-0.1.0-SNAPSHOT.jar ${CLASS_NAME}
  ```

* Run examples through SBT / IDE

  Be sure, that Spark dependency is not marked as `"provided"`, more comments can be found in a [build.sbt](build.sbt) file.

  In [docker-compose.yml](docker-compose.yml) you can notice a commented out section, which start Intelij IDEA 2016.2 Community
  edition in a docker container with X11 socker forwarding. `daunnc/idea:mesatrellis` is an image with downloaded most of the required
  java deps into local maven repos. However you can use other tags, which contain "clean" images.

## GeoTrellis examples

For GeoTrellis tests you need tiles, it is possible Landsat 8 tiles. Prepared instructions can be found [here](data/landsat).

## Possible issues

Running [GeoDocker Cluster](https://github.com/geodocker/geodocker) on Windows be sure that you have `libxml2.dll`
and `libzma-5.dll` in your `Windows/System32` and `Windows/SysWOW64` folders. It is necessary for correct
[GeoDocker Accumulo](https://github.com/geodocker/geodocker-accumulo) start.

## Runing [GeoDocker Cluster](https://github.com/geodocker/geodocker) distributed

We [prepared](.docker) decomposed docker-compose files for each process (separated, experimental). The idea is to map docker network on machines network. From the one hand that makes possible to get rid of network overhead, from the other makes easier to understnad how processes resolve addresses.

To run each process use: `dokcer-compose -f dockercomposefilename.yml up`

Be carefull with docker compose files settings, for each container should be setted: `${HADOOP_MASTER_ADDRESS}` and `${ZOOKEEPERS}`, all docker compose files use host machine network, that means that additional attention should be payed to possible ports conflicts.

Containers hould be started in the following order:

* Zookeepers (on each node, or on one node; current docker compose makes possible to run it only in a single node mode)
* Hadoop 
  * [hadoop-name](.docker/hadoop-name.yml), it is a _master_ process
  * [hadoop-sname](.docker/hadoop-sname.yml), it is a _master_ process
  * [hadoop-data](.docker/hadoop-data.yml), it is a _slave_ process, as much as you want / need to have
* Accumulo
  * [accumulo-master](.docker/accumulo-master.yml), it is a _master_ process, inits HDFS Accumulo volumes on the first start
  * [accumulo-tracer](.docker/accumulo-tracer.yml), it's a _master_ process
  * [accumulo-gc](.docker/accumulo-gc.yml), it's a _master_ process
  * [accumulo-monitor](.docker/accumulo-gc.yml), it's a _master_ process (web ui)
  * [accumulo-tserver](.docker/accumulo-tserver.yml), it's a _slave_ process, as much as you want / need to have
* Spark
  * [spark-master](.docker/spark-master.yml), _master_ process
  * [spark-worker](.docker/spark-worker.yml), _slave_ process

And (in addition, a separate GUI container):
* Intelij IDEA
  * [idea](.docker/idea.yml), be carefull with its configs

## License

* Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
