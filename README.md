# GeoTrellis GeoMesa Template (MesaTrellis)

[![Join the chat at https://gitter.im/geotrellis/geotrellis](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/geotrellis/geotrellis?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Libs / Supported environment

* Java 8
* Scala 2.11.8
* [GeoTrellis](https://github.com/geotrellis/geotrellis) `1.0.0-SNAPSHOT`
* [GeoMesa](https://github.com/locationtech/geomesa/) `1.2.6`
* [GeoDocker cluster](https://github.com/geotrellis/geodocker-cluster) `latest`
* [Spark](http://spark.apache.org/) `2.x`
* [Hadoop](http://hadoop.apache.org/) `2.7.x`
* [Accumulo](http://accumulo.apache.org/) `1.7.x`

## Tutorial description (each example has additional informtion in comments)

* [HelloWorld.scala](src/main/scala/com/azavea/mesatrellis/HelloWorld.scala)
  * A common scala HelloWorld
* [SparkHelloWorld.scala](src/main/scala/com/azavea/mesatrellis/spark/SparkHelloWorld.scala)
  * A Spark HelloWorld
* [MultibandLandsatIngest.scala](src/main/scala/com/azavea/mesatrellis/spark/MultibandLandsatIngest.scala)
  * An example of a hand written ingest job
* [CreateNDVIPng.scala](src/main/scala/com/azavea/mesatrellis/raster/CreateNDVIPng.scala)
  * An example of a hand written ingest job
* [GeoMesaQuery](src/main/scala/com/azavea/mesatrellis/feature/GeoMesaQuery.scala)
  * An example of GeoMesa ingesting / querying

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

There are many ways to deploy the required components depending on the load the cluster will be facing and existing infrastructure.
Generally it is safe to collocate all the `master` services on one host and scale `worker` nodes.
As each component becomes a bottleneck or competes for resources it may be split out into its own node.
Likewise while the `worker` processes benefit from collocation they may be spread over individual nodes and indeed over clusters.

We prepared decomposed docker-compose files that illustrate the minimum separation between nonscalable, [master](./.docker/master.yml) components and [worker](./.docker/worker.yml) components. If we map the docker network to machines network these docker-compose files may be used as bases for deployment.

To run each process use: `dokcer-compose -f dockercomposefilename.yml up`

However this does not solve the resource discovery problem among these components and central pieces, like HDFS master address and Zookeeper address, must be provided as parameters.
Each container should at least set: `${HADOOP_MASTER_ADDRESS}` and `${ZOOKEEPERS}` environment variables.
All docker-compose files use host machine network.
This means that additional attention should be payed to possible ports conflicts.

Containers hould be started in the following order:

### Zookeeper

 * [zookeeper](.docker/zookeeper.yml)(single/scale)

Usually one instance is sufficient.
Multiple instance provide high-availability as hot-standby.

### Hadoop HDFS

  * [hadoop-name](.docker/hadoop-name.yml) (single)
 HDFS Namenode, provides filesystem directory service.
  * [hadoop-sname](.docker/hadoop-sname.yml) (single)
 HDFS Secondary Namenode, provides HDFS checkpoints when merging the HDFS editlogs with fsimage.
  * [hadoop-data](.docker/hadoop-data.yml) (multiple/scale)
 HDFS Datanode, manages HDFS block storage and serves clients referred by namenode, scale for added storage.

All roles of HDFS cluster are configured by their copies of `core-site.xml` and `hdfs-site.xml` files.
`HADOOP_MASTER_ADDRESS` environment variable is used to generate this bare-bone configuration.
In all cases `HADOOP_MASTER_ADDRESS` should be the ip/hostname where `hadoop-name` container is running.

### Accumulo

  * [accumulo-master](.docker/accumulo-master.yml) (single)
  Provides tablet directory, provides central query point, delegates queries to tablet servers, re-balances the Accumulo cluster.
  * [accumulo-tracer](.docker/accumulo-tracer.yml) (single/optional)
  Collects tracers from query clients for debugging.
  * [accumulo-gc](.docker/accumulo-gc.yml), (single/multiple)
  Removes Accumulo HDFS files no longer in use by Accumulo. Multiple instances provide hot-standby.
  * [accumulo-monitor](.docker/accumulo-gc.yml) (single/multiple)
  Provides Accumulo cluster status Web UI page. Multiple instances provide hot-standby.
  * [accumulo-tserver](.docker/accumulo-tserver.yml)(multiple/scale)
  Manages Accumulo tablets on HDFS, components of an accumulo table. Has in-memory record cache.

#### Reference
  * [Accumulo Architecture](http://accumulo.apache.org/1.6/accumulo_user_manual#_components)

#### Dependencies
  * [Hadoop HDFS](#Hadoop-HDFS): tablet file storage, shared class-path
  * [Zookeeper](#Zookeeper): Instance configuration, authentication, shared cluster state

Accumulo requires valid Hadoop configuration, at a minimum `core-site.xml`.
This file is generated from `HADOOP_MASTER_ADDRESS` in a same manner used for HDFS containers themselves.
Alternatively if a valid HDFS configuration already exists, for instance if HDFS is not provided by GeoDocker containers, it may be volume mounted to these containers on `/etc/hadoop/conf`.

Accumulo tserver containers find Accumulo master through lookup of `INSTANCE_NAME` in `ZOOKEEPERS`.
Similarly Accumulo clients, like spark jobs, required a zookeeper address along with instance name to find and query the Accumulo master.

#### Collocation

`accumulo-tserver` benefits with being collocated with `hadoop-data` containers. However, this is not required and tablet server in-memory cache is designed to mitigate scenarios where these services are not collocated.

### Spark

  * [spark-master](.docker/spark-master.yml) (single)
  Provides cluster manager, scheduling spark tasks to be run on available executors.
  * [spark-worker](.docker/spark-worker.yml) (single/scale)
  Spark worker is a container for spark executors which in turn execute specific spark job tasks.

These containers provide [Spark standalone cluster](http://spark.apache.org/docs/latest/spark-standalone.html).
Alternatives include deploying spark through [YARN](http://spark.apache.org/docs/latest/running-on-yarn.html) and [Mesos](http://spark.apache.org/docs/latest/running-on-mesos.html).

Currently GeoDocker does not provide containers for deploying YARN or Mesos.

#### Collocation

`spark-worker` (or any spark executor in general) benefits from being collocated with HDFS datanodes.
When reading files directly from HDFS spark tasks will be distributed with preference for executors that are hosted on the same node as the HDFS blocks.
This mechanism uses the configured host machine name to decide when an executor is collocated with HDFS block.
Therefore it is critical that in those cases both HDFS and spark services are bound to the same interface.

`spark-worker` also benefits from being collocated with Accumulo tserver.
During query planning the client queries Accumulo master, which provides tablet distribution, to determine how to distribute tasks that will read from Accumulo.


### Intelij IDEA / Spark Driver
  * [idea](.docker/idea.yml)
  Driver program, launched either by `spark-submit` or IntelLiJ IDEA
  _Note_: be mindful of configurations for Idea container

Some container/machine will start the JVM that creates the `SparkCotnext`, this is the driver program.
It will communicate with `spark-master` to request executor resources and then with `spark-worker` containers during task execution.
In spark standalone mode this process will always be outside of the Spark executors.
Notably YARN cluster mode YARN is asked to allocate a YARN container for the driver which may be placed on one of the YARN workers, alongside a YARN container hosting a spark executor.

This container should have a stable network connection to `spark-master` and `spark-executor`s as it will be pushing tasks and collecting their results.


## License

* Licensed under the Apache License, Version 2.0: http://www.apache.org/licenses/LICENSE-2.0
