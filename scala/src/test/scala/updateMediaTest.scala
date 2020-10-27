package anilist.recsystem

import  org.scalatest.flatspec.AnyFlatSpec
import  org.scalatest.matchers.should._

class UpdateMediaTest extends AnyFlatSpec with Matchers {
    "collect info" should "not fail" in {
        anilist.recsystem.UpdateMediaInfo.collectMediaInfo(1)
    }

    //val result = anilist.recsystem.UpdateMediaInfo.collectInfo(1)
    //result should  include """{"errors":[{"message":"No query or mutation provided. Please view https:\/\/anilist.co\/graphiql to see available queries and mutations.","status":400,"locations":[{"line":1,"column":1}]}],"data":null}""" ()

    "result" should "not be error line" in {
        val res = anilist.recsystem.UpdateMediaInfo.collectMediaInfo(1) 
        //println(res)
        res should startWith ("""{"data":{"Media":{"id":1,"type":"ANIME","title":{"romaji":"Cowboy Bebop","english":"Cowboy Bebop","native":""")
    }  

    "collect user info" should "not fail" in {
        anilist.recsystem.UpdateMediaInfo.collectUserInfo(1,"ANIME")
    }

    "userinfo" should "not be error line" in {
        val res = anilist.recsystem.UpdateMediaInfo.collectUserInfo(1,"ANIME") 
        //println(res)
        res should startWith ("""{"data":{"MediaListCollection":{"lists":[{"entries":[{"userId":1""")
    }  

    "collect user info by name" should "not fail" in {
        anilist.recsystem.UpdateMediaInfo.collectUserInfoByName("gazavat","MANGA")
    }

    
    "user info" should "not be error line" in {
        val res = anilist.recsystem.UpdateMediaInfo.collectUserInfoByName("gazavat","MANGA") 
        //println(res)
        res should startWith ("""{"data":{"MediaListCollection":{"lists":[{"entries":[{"userId":162707""")
    }  
}
