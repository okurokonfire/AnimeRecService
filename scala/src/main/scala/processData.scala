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

object ProcessData {
    Logger.getLogger("org.apache.spark").setLevel(Level.WARN)
    val spark = {
        SparkSession
            .builder()
            .config("spark.master", "local[1]")
            .getOrCreate()
        }
    //spark.conf.set("spark.conf_dir.postgres.location","/home/gazavat/git/AnimeRecService/postgres")
    val postgres_dir_location = spark.conf.get("spark.conf_dir.postgres.location")

    def readFile(path: String) = {
        val source = scala.io.Source.fromFile(path)
        try source.mkString finally source.close()
    }

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
            
        val sslkey = if ((new java.io.File(s"${postgres_dir_location}/postgresql.key")).exists){
            s"${postgres_dir_location}/postgresql.key"
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
        val params = getSQLParams()
        val url = s"jdbc:postgresql://${params("host")}:${params("port")}/${params("dbname")}?user=${params("user")}&sslcert=${params("sslcert")}&sslkey=${params("sslkey")}&sslrootcert=${params("sslrootcert")}&sslmode=${params("sslmode")}"

        DriverManager.getConnection(url)

    }


}