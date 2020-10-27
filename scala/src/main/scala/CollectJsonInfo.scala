package anilist.recsystem


//import io.circe._
//import io.circe.parser._
//import com.google.gson.Gson
import play.api.libs.json._
import scala.util.Try

import org.apache.http.client.methods.HttpPost
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import java.nio.charset.StandardCharsets
//import java.sql.{Connection, DriverManager, ResultSet}

object CollectJsonInfo {
    def collectInfo(params: String) = {
        val url = "https://graphql.anilist.co"
        val post = new HttpPost(url)
        post.addHeader("content-type","application/json")
        post.setEntity(new StringEntity(params.replace("\n","")))

        val client = new DefaultHttpClient
        val response = client.execute(post)

        val result = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8)

        result
    }


    def collectMediaInfo(mediaID: Int) = {
        val params = s"""
        {"query": "query ($$id: Int)
            { Media(id: $$id , id_not:0)
              {
                id  
                type  
                title 
                {  
                  romaji  
                  english  
                  native  
                }  
                synonyms  
                startDate 
                {  
                  year  
                  month  
                  day  
                }  
                endDate 
                {  
                  year  
                  month  
                  day  
                }  
                episodes  
                duration
                chapters
                volumes
                format  
                tags
                {  
                  id  
                  name  
                  category  
                }  
                genres  
                source  
                status  
                studios
                {
                  edges
                  {
                    id
                    isMain
                    node
                    {
                      id
                      name
                    }
                  }
                }
                staff {
                  edges{
                    id
                    role
                    node{
                      id
                      name {
                        first
                        last
                        full
                        native
                      }
                    }
                  }
                }
              }
            }"
        , "variables": "{$$id : $mediaID}"
        }
        """
        collectInfo(params)
        
    }

    def collectUserInfo(userID: Int, mediaType: String) = {
        val params = s"""
        {"query": " query ($$userId: Int)   
          {  MediaListCollection(userId: $$userId, type: ${mediaType}) 
          {  lists 
            {  entries 
              { userId  
                id  
                mediaId  
                startedAt {  year  month  day  }  
                completedAt {  year  month  day  }  
                score  
                progress  
                repeat  
                media {  id  type  }  
              }  
            }  
          }  
        }", "variables": {"userId" : $userID }}
        """
        collectInfo(params)
    }

    def collectUserInfoByName(name:String,mediaType:String) = {
        val params = s"""
        {"query": " query ($$name: String)   
          { User(name: $$name)
              {
                id
              }
            }", "variables": {"name" : "$name" }}
        """
        val userinfo = collectInfo(params)
        val json: JsValue = Json.parse(userinfo)
        val userId = Try((json \ "data" \ "User" \ "id").get.toString.toInt).getOrElse(throw new Exception("user is incorrect"))
        collectUserInfo(userId,mediaType)
    }
  //def main(args: Array[String]): Unit = {
  //    println("Hello, World!")
  //}
}