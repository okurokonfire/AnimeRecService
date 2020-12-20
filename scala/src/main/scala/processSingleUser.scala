package anilist.recsystem

import play.api.libs.json._
import scala.util.Try
import java.sql.Connection
import java.sql.Statement

import anilist.recsystem.Utils._
import anilist.recsystem.ProcessData._
import anilist.recsystem.CollectJsonInfo._



object ProcessDataSingleUser {
    //spark
    //spark.conf.set("spark.conf_dir.postgres.location","/home/gazavat/git/AnimeRecService/postgres")
    //spark.conf.set("spark.conf_dir.kafka.location","/home/gazavat/git/AnimeRecService/kafka")
    def uploadSingleUserById(id: Int) = {
        val userInfo = s"""{"anime": ${collectUserInfo(id,"ANIME")}  ,"manga": ${collectUserInfo(id,"MANGA")}   }"""
        processUserInfo(userInfo)
    }
    def uploadSingleUserByName(name: String) = {
        val userInfo = s"""{"anime": ${collectUserInfoByName(name,"ANIME")}  ,"manga": ${collectUserInfoByName(name,"MANGA")}   }"""
        processUserInfo(userInfo)
    }

    def generateRecsForUser(id: Int): List[MediaRecommendations] = {
        ???
    }
    def getRecommendationForUserById(id: Int, params: play.api.libs.json.JsValue) = {
        val sql_connection = getSQLConnection
        sql_connection.setAutoCommit(false)
        sql_connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ)
        val stmt = sql_connection.createStatement()
        Try({
            val query = s"""
            select u.userid, u.anilistuserid, a.animeid, a.anilistanimeid, a.title, ar.score
              from tuser u
        inner join tanimerecs ar 
                on ar.userid = u.userid 
        inner join tanime a
                on a.animeid = ar.animeid
             where u.anilistuserid = $id
            """
            val rs = stmt.executeQuery(query)
            val existing_recs = new Iterator[MediaRecommendations]{
                def hasNext = rs.next()
                def next()  = MediaRecommendations(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getInt(4), rs.getString(5), rs.getInt(6))
            }.toList.sortBy(_.score)
            val recalc = (params \ "refresh").asOpt[String] match {
                case Some("true") => 1
                case _ => 0
            }
            val recalc_exist = existing_recs.size match {
                case 0 => 1
                case _ => 0
            }
            val rest_params = params.as[JsObject].fieldSet.filter(_._1!="refresh")
            val recalc_rest = rest_params.size match {
                case 0 => 1
                case _ => 0
            }

            val new_recalc = recalc + recalc_exist + recalc_rest
            val new_recs   = new_recalc match {
                case 0 => existing_recs
                case _ => {
                    uploadSingleUserById(id)
                    generateRecsForUser(id)
                }
            }
            sql_connection.commit
            new_recs
        }).getOrElse({sql_connection.rollback; throw new Exception(s"Error while getting recommendations for user ${id}")})

    }
    def getRecommendationForUserByName(name: String, params: play.api.libs.json.JsValue) = {
        val namequery = s"""
        {"query": " query ($$name: String)   
          { User(name: $$name)
              {
                id
              }
            }", "variables": {"name" : "$name" }}
        """
        val userinfo = collectInfo(namequery)
        val json: JsValue = Json.parse(userinfo)
        val userId = Try((json \ "data" \ "User" \ "id").get.toString.toInt).getOrElse(throw new Exception("user is incorrect"))
        getRecommendationForUserById(userId,params)
    }
    def main(args: Array[String]): Unit = {
        val mode = Try(args(0)).getOrElse(throw new Exception("you should declare mode"))
        val arg  = Try(args(1)).getOrElse(throw new Exception("you should declare user id or name"))
        val params: JsValue = Try(Json.parse(args.slice(2,args.size).mkString(" ")) ).getOrElse(throw new Exception("incorrect json string"))
        //{"format":["TV","TV_SHORT"],"genres":["Action","Slice of Life"],"tags":["Martial Arts","Tsundere"],"mediatype":["ANIME","MANGA"],"started before":"2020-12-12","started after":"2000-01-01","ended before":"2024-10-10","ended after": "2000-10-11","refresh":"true"}
        mode match {
            case "id"    => getRecommendationForUserById(arg.toInt,params)
            case "name"  => getRecommendationForUserByName(arg,params)
            case _       => throw new Exception("incorrect mode")
        }
    }
}
