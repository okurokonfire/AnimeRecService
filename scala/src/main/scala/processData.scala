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

import anilist.recsystem.Utils._

object ProcessData {
    case class MediaTag (id: Int, name: String, category: String)
    case class MediaStudio (id: Int, name: String, isMain: Boolean)

    def getDate(date: play.api.libs.json.JsValue) = {
        val (year,month,day) = (isnull(date("year"),1900),isnull(date("month"),1),isnull(date("day"),1)) 
        val newDate = Try(java.time.LocalDate.of(year,month,day)).getOrElse(java.time.LocalDate.of(1900,1,1))
        newDate.toString
    }
    
    def processMediaInfo(input: String) = {
        val sql_connection = getSQLConnection
        sql_connection.setAutoCommit(false)
        sql_connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)
        val stmt = sql_connection.createStatement()
        //main body
        Try({
            val json: JsValue = Json.parse(input)
            val media = json \ "data" \ "Media"
            val anilistMediaID = media("id").toString.toInt
            val mediaType = media("type").toString
            val title = media("title")
            val romajiTitle = title("romaji").toString
            val englishTitle = title("english").toString
            val nativeTitle = title("native").toString
            val synonyms = media("synonyms").as[JsArray].value.map(synonym => {
                synonym.toString
            })
            val startDate = getDate(media("startDate"))
            val endDate = getDate(media("endDate"))
            val episodes = Try(media("episodes").toString.toInt).getOrElse(0)
            val duration = Try(media("duration").toString.toInt).getOrElse(0)
            val chapters = Try(media("chapters").toString.toInt).getOrElse(0)
            val volumes = Try(media("volumes").toString.toInt).getOrElse(0)
            val format = media("format").toString
            val tags = media("tags").as[JsArray].value.map(line => {
                MediaTag(line("id").toString.toInt,line("name").toString,line("category").toString)
            })
            val genres = media("genres").as[JsArray].value.map(genre => {
                genre.toString
            })
            val source = media("source").toString
            val status = media("status").toString

            val studios = media("studios")("edges").as[JsArray].value.map(line => {
                MediaStudio(line("node")("id").toString.toInt,line("node")("name").toString,line("isMain").toString.toBoolean)
            })


            sql_connection.commit()
        }).getOrElse(throw new Exception(s"Error while processing ${input}"))
        sql_connection.close()
    }


}