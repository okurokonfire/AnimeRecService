name := "ARS"

scalaVersion := "2.12.12"

version := "0.1.0"

/*libraryDependencies += "com.google.code.gson" % "gson" % "2.8.6"

libraryDependencies += "io.circe" %% "circe-parser" % "0.13.0"
*/

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.8.1"

libraryDependencies += "org.postgresql" % "postgresql" % "9.4.1212"

libraryDependencies += "org.apache.spark" %% "spark-core" % "3.0.1"

libraryDependencies += "org.apache.spark" %% "spark-sql" % "3.0.1"

libraryDependencies += "org.apache.spark" %% "spark-mllib" % "3.0.1" % "provided"

libraryDependencies += "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.0.1" % "provided"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.0" % "test" 
libraryDependencies += "org.scalactic" %% "scalactic" % "3.2.0"

scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-encoding", "UTF-8",
  "-unchecked",
  "-Xlint",
)

fork := true

javaOptions += "-Dfile.encoding=UTF-8"