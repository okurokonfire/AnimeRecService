package anilist.recsystem

import play.api.libs.json._
import scala.util.Try
import org.apache.log4j.{Level, Logger}
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.streaming.Trigger
import java.sql.{Connection, DriverManager, ResultSet, Statement}

import anilist.recsystem.Utils._

object ProcessData {
    case class MediaTag (id: Int, name: String, category: String)
    case class MediaStudio (id: Int, name: String, isMain: Boolean)
    case class MediaStaff (id: Int, firstName: String, lastName: String, fullName: String, nativeName: String, role: String)

    def updateMedia() = {
        ???
    }

    def insertMedia() = {
        ???
    }

    def getMediaTypeId(stmt: Statement, mediaName: String): Int = {
        val query = s"select mediatypeid from public.tmediatype where name = '${mediaName}';"
        val rs = stmt.executeQuery(query)
        rs.next match {
            case true => rs.getInt(1)
            case false => {
                val q = s"insert into public.tmediatype(name) select '${mediaName}';"
                val rs0 = stmt.executeUpdate(q)
                val rs1 = stmt.executeQuery(query)
                if (rs1.next) {
                    rs1.getInt(1)
                } else {
                    throw new Exception(s"something went wrong while inserting mediaType = $mediaName")
                }
            }
        }
    }

    def getDate(date: play.api.libs.json.JsValue) = {
        val (year,month,day) = (anilist.recsystem.Utils.isnull(date("year"),1900),anilist.recsystem.Utils.isnull(date("month"),1),anilist.recsystem.Utils.isnull(date("day"),1)) 
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
            val mediaType = media("type").toString.replace("\"","")
            val mediaTypeId = getMediaTypeId(stmt,mediaType)
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

            val staff = media("staff")("edges").as[JsArray].value.map(line => {
                MediaStaff(line("node")("id").toString.toInt,line("node")("name")("first").toString,line("node")("name")("last").toString,line("node")("name")("full").toString,line("node")("name")("native").toString,line("role").toString)
            })

            val rs = stmt.executeQuery(s"select animeid from public.tanime where anilistanimeid = $anilistMediaID;")

            if (rs.next) {
                updateMedia()
            } else {
                insertMedia()
            }

            sql_connection.commit()
        }).getOrElse(throw new Exception(s"Error while processing ${input}"))
        sql_connection.close()
    }


}