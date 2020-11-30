package anilist.recsystem

import play.api.libs.json._
import scala.util.Try
import org.apache.log4j.{Level, Logger}
//import org.apache.spark.sql.SparkSession
//import org.apache.spark.sql.functions._
//import org.apache.spark.sql.types._
//import org.apache.spark.sql.DataFrame
//import org.apache.spark.sql.streaming.Trigger
import java.sql.{Connection, DriverManager, ResultSet, Statement}

import anilist.recsystem.Utils._

object ProcessData {
    case class MediaTag (id: Int, name: String, category: String)
    case class MediaStudio (id: Int, name: String, isMain: Boolean)
    case class MediaStaff (id: Int, firstName: String, lastName: String, fullName: String, nativeName: String, role: String)
    case class Anime (animeListId: Int, title: String, dateStart: String, dateEnd: String, episodes: Int, duration: Int, chapters: Int, volumes: Int, formatID: Int, sourceID: Int, statusID: Int, mediaTypeId: Int)
    case class MediaNames (romaji: String, english: String, native: String, synonyms: IndexedSeq[String])

    def updateMedia(anime: Anime, animeNames: MediaNames, staff: Set[MediaStaff], tags: Set[MediaTag], studios: Set[MediaStudio]) = {
        ???
    }

    def insertMedia(anime: Anime, animeNames: MediaNames, staff: Set[MediaStaff], tags: Set[MediaTag], studios: Set[MediaStudio]) = {
        ???
    }


    def getIdByNameType(stmt: Statement,`type` : String,  name: String): Int = {
        val query = s"select ${`type`}id from public.t${`type`} where name = '${name}';"
        val rs = stmt.executeQuery(query)
        rs.next match {
            case true => rs.getInt(1)
            case false => {
                val q = s"insert into public.t${`type`}(name) select '${name}';"
                stmt.executeUpdate(q)
                val rs1 = stmt.executeQuery(query)
                if (rs1.next) {
                    rs1.getInt(1)
                } else {
                    throw new Exception(s"something went wrong while inserting ${`type`} = $name")
                }
            }
        }
    }

    def cleanTitles(input: String) : String = input match {
        case "null" => "null"
        case x => input.slice(1,input.length - 1)
    }

    def getDate(date: play.api.libs.json.JsValue) = {
        val (year,month,day) = (anilist.recsystem.Utils.isnull(date("year"),1900),anilist.recsystem.Utils.isnull(date("month"),1),anilist.recsystem.Utils.isnull(date("day"),1)) 
        val newDate = Try(java.time.LocalDate.of(year,month,day)).getOrElse(java.time.LocalDate.of(1900,1,1))
        newDate.toString
    }

    def processAnime(stmt: Statement, anime: Anime): Int ={
        val anilistID = anime.animeListId
        val query = s"select animeid, anilistanimeid, title, datestart, dateend, episodes, duration, chapters, volumes, formatid, sourceid, statusid, mediatypeid from tanime where anilistanimeid = $anilistID"
        val rs = stmt.executeQuery(query)
        rs.next match {
            case true  => {
                val (animeid, curr_anime) = (rs.getInt(1),Anime(rs.getInt(2),rs.getString(3),rs.getDate(4).toString,rs.getDate(5).toString,rs.getInt(6),rs.getInt(7),rs.getInt(8),rs.getInt(9),rs.getInt(10),rs.getInt(11),rs.getInt(12),rs.getInt(13)))
                (curr_anime == anime) match {
                    case true  => animeid
                    case false => {
                        val updateQuery = s"""
                            update public.tanime 
                            set 
                               	anilistanimeid = '${anime.animeListId}',
	                            title          = '${anime.title}'      ,
	                            datestart      = '${anime.dateStart}'  ,
	                            dateend        = '${anime.dateEnd}'    ,
	                            episodes       = '${anime.episodes}'   ,
	                            duration       = '${anime.duration}'   ,
	                            chapters       = '${anime.chapters}'   ,
	                            volumes        = '${anime.volumes}'    ,
	                            formatid       = '${anime.formatID}'   ,
	                            sourceid       = '${anime.sourceID}'   ,
	                            statusid       = '${anime.statusID}'   ,
	                            mediatypeid    = '${anime.mediaTypeId}'
                            where animeid = ${animeid}
                            ;
                        """
                        stmt.executeUpdate(updateQuery)
                        animeid
                    }
                }
            }
            case false => {
                val insertQuery = s"""
                    insert into public.tanime 
                    (
                    	anilistanimeid ,
	                    title,
	                    datestart,
	                    dateend,
	                    episodes,
	                    duration,
	                    chapters,
	                    volumes,
	                    formatid,
	                    sourceid,
	                    statusid,
	                    mediatypeid
                    )  
                    select 
                    	'${anime.animeListId}' ,
	                    '${anime.title}',
	                    '${anime.dateStart}',
	                    '${anime.dateEnd}',
	                    '${anime.episodes}',
	                    '${anime.duration}',
	                    '${anime.chapters}',
	                    '${anime.volumes}',
	                    '${anime.formatID}',
	                    '${anime.sourceID}',
	                    '${anime.statusID}',
	                    '${anime.mediaTypeId}'
                    ;
                """
                stmt.executeUpdate(insertQuery)
                val rs0 = stmt.executeQuery(query)
                if (rs0.next) {
                    rs0.getInt(1)
                } else {
                    throw new Exception(s"something went wrong while inserting anime = $anime")
                }
            }
        }
    }

    def linkAnimeGenre(stmt: Statement, animeid: Int, genreName: String) = {
        val genreID = getIdByNameType(stmt, "genre", genreName)
        val linkQuery = s"""
        insert into tgenrelist 
        (
            genreid,
            animeid
        )
        select
            $genreID,
            $animeid;"""
        stmt.executeUpdate(linkQuery)
    }

    def unlinkAnimeGenre(stmt: Statement, animeid: Int, genreName: String) = {
        val genreID = getIdByNameType(stmt, "genre", genreName)
        val linkQuery = s"""
        delete from tgenrelist 
        where genreid = $genreID
          and animeid = $animeid;"""
        stmt.executeUpdate(linkQuery)
    }

    def processGenres(stmt: Statement, animeid: Int, genres: Set[String]) = {
        val query = s"""
            select g.name 
              from tgenrelist gl
        inner join tgenre g
                on gl.genreid = g.genreid
             where gl.animeid = $animeid
        """
        val rs = stmt.executeQuery(query)

        val existed_genres = new Iterator[String] {
                    def hasNext = rs.next()
                    def next()  = rs.getString(1)
                }.toSet
        (genres == existed_genres) match {
            case true  => Set(0)
            case false => {
                val extra = existed_genres.diff(genres)
                val new_genres = genres.diff(existed_genres)
                new_genres.map( x =>{
                    linkAnimeGenre(stmt,animeid,x)
                })
                extra.map(x => {
                    unlinkAnimeGenre(stmt,animeid,x)
                })
            }
        }
    }
    
    def processMediaInfo(input: String) = {
        //val input = anilist.recsystem.CollectJsonInfo.collectMediaInfo(722)
        val sql_connection = getSQLConnection
        sql_connection.setAutoCommit(false)
        sql_connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)
        val stmt = sql_connection.createStatement()
        //main body
        Try({
            val json: JsValue = Json.parse(input)
            val media = json \ "data" \ "Media"
            val anilistMediaID = media("id").toString.toInt
            val mediaType = cleanTitles(media("type").toString)
            val mediaTypeId = getIdByNameType(stmt,"mediatype",mediaType)
            val title = media("title")
            val romajiTitle = cleanTitles(title("romaji").toString)
            val englishTitle = cleanTitles(title("english").toString)
            val nativeTitle = cleanTitles(title("native").toString)
            val synonyms = media("synonyms").as[JsArray].value.map(synonym => {
                synonym.toString
            }).toSet
            val startDate = getDate(media("startDate"))
            val endDate = getDate(media("endDate"))
            val episodes = Try(media("episodes").toString.toInt).getOrElse(0)
            val duration = Try(media("duration").toString.toInt).getOrElse(0)
            val chapters = Try(media("chapters").toString.toInt).getOrElse(0)
            val volumes = Try(media("volumes").toString.toInt).getOrElse(0)
            val format = cleanTitles(media("format").toString)
            val formatID = getIdByNameType(stmt,"format",format)
            val tags = media("tags").as[JsArray].value.map(line => {
                MediaTag(line("id").toString.toInt,cleanTitles(line("name").toString),cleanTitles(line("category").toString))
            }).toSet
            val genres = media("genres").as[JsArray].value.map(genre => {
                cleanTitles(genre.toString)
            }).toSet
            val source = cleanTitles(media("source").toString)
            val sourceID = getIdByNameType(stmt, "source",source)


            val status = cleanTitles(media("status").toString)
            val statusID = getIdByNameType(stmt,"status", status)

            val studios = media("studios")("edges").as[JsArray].value.map(line => {
                MediaStudio(line("node")("id").toString.toInt,line("node")("name").toString,line("isMain").toString.toBoolean)
            }).toSet

            val staff = media("staff")("edges").as[JsArray].value.map(line => {
                MediaStaff(line("node")("id").toString.toInt,line("node")("name")("first").toString,line("node")("name")("last").toString,line("node")("name")("full").toString,line("node")("name")("native").toString,line("role").toString)
            }).toSet

            val anime = Anime(anilistMediaID, romajiTitle, startDate, endDate, episodes, duration, chapters, volumes, formatID, sourceID, statusID, mediaTypeId)

            val animeid = processAnime(stmt, anime)

            processGenres(stmt, animeid, genres)


            sql_connection.commit()
        }).getOrElse(throw new Exception(s"Error while processing ${input}"))
        sql_connection.close()
    }


}