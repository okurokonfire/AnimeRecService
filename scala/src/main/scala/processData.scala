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

    def cleanTitles(input: String): String = {
        val start = input.head match {
            case '\"' => 1
            case _    => 0
        }
        val end = input.last match {
            case '\"' => input.length - 1
            case _    => input.length
        }
        input.slice(start,end)
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
            case true  => Set(-1)
            case false => {
                val extra = existed_genres.diff(genres)
                val new_genres = genres.diff(existed_genres)
                extra.map(x => {
                    unlinkAnimeGenre(stmt,animeid,x)
                })
                new_genres.map( x =>{
                    linkAnimeGenre(stmt,animeid,x)
                })
            }
        }
    }

    def getTagId(stmt: Statement, tag: MediaTag) : Int = {
        val query = s"""
        select tagid
        from ttag
        where anilisttagid = ${tag.id}
        """
        val rs = stmt.executeQuery(query)
        rs.next match {
            case true  => rs.getInt(1)
            case false => {
                val categoryId = getIdByNameType(stmt,"tagcategory", tag.category)
                val insertQuery = s"""
                insert into ttag
                (
                    anilisttagid,
                    categoryid,
                    name
                )
                select
                    ${tag.id},
                    ${categoryId},
                    '${tag.name}'
                """
                stmt.executeUpdate(insertQuery)
                val rs0 = stmt.executeQuery(query)
                if (rs0.next) {
                    rs0.getInt(1)
                } else {
                    throw new Exception(s"something went wrong while inserting tag = $tag")
                }
            }
        }
    }
       

    def linkAnimeTag(stmt: Statement, animeid: Int, tag: MediaTag) = {
        val tagid = getTagId(stmt,tag)
        val linkQuery = s"""
        insert into ttaglist 
        (
            tagid,
            animeid
        )
        select
            $tagid,
            $animeid;"""
        stmt.executeUpdate(linkQuery)
    }

    def unlinkAnimeTag(stmt: Statement, animeid: Int, tag: MediaTag) = {
        val tagid = getTagId(stmt,tag)
        val linkQuery = s"""
        delete from ttaglist 
        where tagid = $tagid
          and animeid = $animeid;"""
        stmt.executeUpdate(linkQuery)
    }



    def processTags(stmt: Statement, animeid: Int, tags: Set[MediaTag]) = {
        val query = s"""
            select t.anilisttagid, t.name, tc.name
              from ttaglist tl
        inner join ttag t 
                on tl.tagid = t.tagid 
        inner join ttagcategory tc
                on tc.categoryid = t.categoryid 
             where tl.animeid  = $animeid
        """
        val rs = stmt.executeQuery(query)

        val existed_tags = new Iterator[MediaTag] {
                    def hasNext = rs.next()
                    def next()  = MediaTag(rs.getInt(1),rs.getString(2),rs.getString(3))
                }.toSet
        (tags == existed_tags) match {
            case true  => Set(-1)
            case false => {
                val extra = existed_tags.diff(tags)
                val new_tags = tags.diff(existed_tags)
                extra.map(x => {
                    unlinkAnimeTag(stmt,animeid,x)
                })
                new_tags.map( x =>{
                    linkAnimeTag(stmt,animeid,x)
                })
            }
        } 
    }

    def getStudioId(stmt: Statement, studio: MediaStudio) = {
        val query = s"""
        select studioid
        from tstudio
        where aniliststudioid = ${studio.id}
        """
        val rs = stmt.executeQuery(query)
        rs.next match {
            case true  => rs.getInt(1)
            case false => {
                val insertQuery = s"""
                insert into tstudio
                (
                    aniliststudioid,
                    name
                )
                select
                    ${studio.id},
                    '${studio.name}'
                """
                stmt.executeUpdate(insertQuery)
                val rs0 = stmt.executeQuery(query)
                if (rs0.next) {
                    rs0.getInt(1)
                } else {
                    throw new Exception(s"something went wrong while inserting studio = $studio")
                }
            }
        }
    }

    def linkAnimeStudio(stmt: Statement, animeid: Int, studio: MediaStudio) = {
        val studioid = getStudioId(stmt,studio)
        val linkQuery = s"""
        insert into tstudiolist 
        (
            studioid,
            animeid,
            ismain
        )
        select
            $studioid,
            $animeid,
            ${studio.isMain}
            ;"""
        stmt.executeUpdate(linkQuery)
    }

    def unlinkAnimeStudio(stmt: Statement, animeid: Int, studio: MediaStudio) = {
        val studioid = getStudioId(stmt,studio)
        val linkQuery = s"""
        delete from tstudiolist 
        where studioid = $studioid
          and animeid = $animeid;"""
        stmt.executeUpdate(linkQuery)
    }

    def processStudios(stmt: Statement, animeid: Int, studios: Set[MediaStudio]) = {
        val query = s"""
        select s.aniliststudioid, s.name, sl.ismain 
          from tstudiolist sl
    inner join tstudio s
            on s.studioid = sl.studioid 
         where sl.animeid = $animeid
        """
        val rs = stmt.executeQuery(query)

        val existed_studios = new Iterator[MediaStudio] {
                    def hasNext = rs.next()
                    def next()  = MediaStudio(rs.getInt(1),rs.getString(2),rs.getBoolean(3))
                }.toSet
        (existed_studios == studios) match {
            case true  => Set(-1)
            case false => {
                val extra = existed_studios.diff(studios)
                val new_studios = studios.diff(existed_studios)
                extra.map(x => {
                    unlinkAnimeStudio(stmt,animeid,x)
                })
                new_studios.map( x =>{
                    linkAnimeStudio(stmt,animeid,x)
                })
            }
        }
    }
    
    def processMediaInfo(input: String) = {
        //val input = anilist.recsystem.CollectJsonInfo.collectMediaInfo(722)
        //spark; spark.conf.set("spark.conf_dir.postgres.location","/home/gazavat/git/AnimeRecService/postgres")
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
                MediaStudio(line("node")("id").toString.toInt,cleanTitles(line("node")("name").toString),line("isMain").toString.toBoolean)
            }).toSet

            val staff = media("staff")("edges").as[JsArray].value.map(line => {
                MediaStaff(line("node")("id").toString.toInt,line("node")("name")("first").toString,line("node")("name")("last").toString,line("node")("name")("full").toString,line("node")("name")("native").toString,line("role").toString)
            }).toSet

            val anime = Anime(anilistMediaID, romajiTitle, startDate, endDate, episodes, duration, chapters, volumes, formatID, sourceID, statusID, mediaTypeId)

            val animeid = processAnime(stmt, anime)

            processGenres(stmt, animeid, genres)

            processTags(stmt, animeid, tags)

            processStudios(stmt, animeid, studios)

            sql_connection.commit()
        }).getOrElse(throw new Exception(s"Error while processing ${input}"))
        sql_connection.close()
    }


}