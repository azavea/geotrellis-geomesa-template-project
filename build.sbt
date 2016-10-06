name := "mesatrellis"
version := "0.1.0-SNAPSHOT"
// Scala version, GeoTrellis master branch is oriented on Scala 2.11 
scalaVersion := "2.11.8"
organization := "com.azavea"
scalacOptions ++= Seq(
  "-deprecation",
  "-unchecked",
  "-Yinline-warnings",
  "-language:implicitConversions",
  "-language:reflectiveCalls",
  "-language:higherKinds",
  "-language:postfixOps",
  "-language:existentials",
  "-feature")

// Resolver is a repository url, to download GeoTrellis snapshot / sha jars
resolvers += Resolver.bintrayRepo("azavea", "geotrellis")

libraryDependencies ++= Seq(
  // GeoTrellis deps, all deps are transitive
  "com.azavea.geotrellis" %% "geotrellis-spark" % "1.0.0-54905b6",
  "com.azavea.geotrellis" %% "geotrellis-accumulo" % "1.0.0-54905b6",
  // Our GeoMesa integration plugin, includes compatible GeoMesa version
  "com.azavea.geotrellis" %% "geotrellis-geomesa" % "1.0.0-54905b6",
  // Spark dep marked as provided in order not to include this artifact into assembly (fat) jar
  // If you want to run application from SBT or from Intelij IDEA / some other IDE
  // be sure that you removed "provided", as app would run a separate standalone Spark driver node
  // which would require spark-core
  "org.apache.spark" %% "spark-core" % "2.0.0" % "provided",
  "org.scalatest"    %%  "scalatest" % "3.0.0" % "test"
)

// When creating fat jar, remote some files with
// bad signatures and resolve conflicts by taking the first
// versions of shared packaged types.
assemblyMergeStrategy in assembly := {
  case "reference.conf" => MergeStrategy.concat
  case "application.conf" => MergeStrategy.concat
  case "META-INF/MANIFEST.MF" => MergeStrategy.discard
  case "META-INF\\MANIFEST.MF" => MergeStrategy.discard
  case "META-INF/ECLIPSEF.RSA" => MergeStrategy.discard
  case "META-INF/ECLIPSEF.SF" => MergeStrategy.discard
  case _ => MergeStrategy.first
}
