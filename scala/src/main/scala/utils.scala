package anilist.recsystem

import play.api.libs.json._
import scala.util.Try
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.streaming.Trigger
import java.sql.{Connection, DriverManager, ResultSet}

object Utils {
    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    val spark = {
        SparkSession
            .builder()
            .config("spark.master", "local[1]")
            .getOrCreate()
        }
    //spark.conf.set("spark.conf_dir.kafka.location","/home/gazavat/git/AnimeRecService/kafka")
    def kafka_dir_location = spark.conf.get("spark.conf_dir.kafka.location")
    def killAll() = {
            SparkSession
            .active
            .streams
            .active
            .foreach { x =>
                        val desc = x.lastProgress.sources.head.description
                        x.stop
                        println(s"Stopped ${desc}")
            }               
        }

    def createConsoleSink(df: DataFrame, 
                              fileName: String) = {
            df
            .writeStream
            .format("console")
            .option("truncate", "true")
            .option("checkpointLocation", s"chk/$fileName")
            .option("numRows", "10")
            .trigger(Trigger.ProcessingTime("10 seconds"))
        }
    
    def createSink(chkName: String, df: DataFrame)(batchFunc: (DataFrame,Long) => Unit) = {
        df
        .writeStream
        .trigger(Trigger.ProcessingTime("10 seconds"))
        .option("checkpointLocation", s"chk/$chkName")
        .foreachBatch(batchFunc)
    }

    def readFile(path: String) = {
        val source = scala.io.Source.fromFile(path)
        try source.mkString finally source.close()
    }

    def getKafkaParams(conf_file: String) = {
        Try({
            val lines = readFile(conf_file)
            lines.split("\n").map(line => {
                val arr = line.split("\\s+")
                arr(0) -> arr(1)
            }).toMap
        }).getOrElse(
            throw new Exception("Kafka conf file is missing or has incorrect format")
        )
    }

    //spark.conf.set("spark.conf_dir.postgres.location","/home/gazavat/git/AnimeRecService/postgres")
    def postgres_dir_location = spark.conf.get("spark.conf_dir.postgres.location")

    def getSQLParams() = {
        val params = Try({
            val lines = readFile(s"${postgres_dir_location}/postgres.conf")
            lines.split("\n").map(line => {
                val arr = line.split("\\s+")
                arr(0) -> arr(1)
            }).toMap
        }).getOrElse(
            throw new Exception("Postgres conf file is missing or has incorrect format")
        )

        val sslcert = if ((new java.io.File(s"${postgres_dir_location}/postgresql.crt")).exists){
            s"${postgres_dir_location}/postgresql.crt"
        } else {
            throw new Exception("sslcert file is missing")
        }
            
        val sslkey = if ((new java.io.File(s"${postgres_dir_location}/postgresql.pk8")).exists){
            s"${postgres_dir_location}/postgresql.pk8"
        } else {
            throw new Exception("sslkey file is missing")
        }        

        val sslrootcert = if ((new java.io.File(s"${postgres_dir_location}/root.crt")).exists){
            s"${postgres_dir_location}/root.crt"
        } else {
            throw new Exception("sslrootcert file is missing")
        }  

        params + ("sslcert" -> sslcert, "sslkey" -> sslkey, "sslrootcert" -> sslrootcert,"sslmode" -> "verify-full")

    }
    def getSQLConnection() = {
        ///DriverManager.getConnection()
        Class.forName("org.postgresql.Driver")
        val params = getSQLParams()
        val url = s"jdbc:postgresql://${params("host")}:${params("port")}/${params("dbname")}?user=${params("user")}&sslcert=${params("sslcert")}&sslkey=${params("sslkey")}&sslrootcert=${params("sslrootcert")}&sslmode=${params("sslmode")}"

        val sql_connection = DriverManager.getConnection(url)
        sql_connection
    }

    def isnull(variable: play.api.libs.json.JsValue, value: Int) = {
        variable.toString.toLowerCase() match {
            case "null" => value
            case _ => variable.toString.toInt
        }
    }
    def getUpdatedKafkaParams(file: String) = {
        val kafkaParams = getKafkaParams(s"${kafka_dir_location}/$file")
        val offsets = Try(kafkaParams("startingOffsets")).getOrElse("earliest")
        val new_offsets = offsets match {
            case "earliest" => "earliest"
            case "latest"   => "latest" 
            case x => s"""{"${kafkaParams("subscribe")}":{"0":${x}}}"""
        }
        kafkaParams.updated("startingOffsets",new_offsets)
    }
    
    case class MediaTag (id: Int, name: String, category: String)
    case class MediaStudio (id: Int, name: String, isMain: Boolean)
    case class MediaStaff (id: Int, firstName: String, lastName: String, fullName: String, nativeName: String, role: String)
    case class Anime (animeListId: Int, title: String, dateStart: String, dateEnd: String, episodes: Int, duration: Int, chapters: Int, volumes: Int, formatID: Int, sourceID: Int, statusID: Int, mediaTypeId: Int)
    case class MediaName (`type`: String, name: String)
    case class MediaListEntry(userId: Int, mediaId: Int, dateStart: String, dateEnd: String, score: Int, progress: Int, repeat: Int, watchStatusId: Int)
    case class MediaRecommendations(userId: Int,anilistUserId: Int,animeId: Int,anilistAnimeId: Int, animeName: String ,score: Int)

    def getConfigFromPostgre(conf: String, mode: String, default: String) = {
        val connection = getSQLConnection()
        val stmt = connection.createStatement()
        val value = mode match {
            case "int" => "value_int"
            case "str" => "value_str"
            case _     => throw new Error("unsupported type for getting config")
        }
        val query = s"""
        select $value
          from counters
         where countername = '$conf'
           and $value is not null
        """
        val rs = stmt.executeQuery(query)
        val res = rs.next match {
            case false => default
            case true  => rs.getString(1)
        }
        connection.close
        res
    }
}