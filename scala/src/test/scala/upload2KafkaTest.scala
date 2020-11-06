package anilist.recsystem

import  org.scalatest.flatspec.AnyFlatSpec
import  org.scalatest.matchers.should._
import org.apache.spark.sql.SparkSession

class Upload2KafkaTest extends AnyFlatSpec with Matchers {
    "upload media info" should "not fail" in {
        val spark = {
            SparkSession
                .builder()
                .config("spark.master", "local[1]")
                .getOrCreate()
            }
        spark.conf.set("spark.conf_dir.kafka.location","/home/gazavat/git/AnimeRecService/kafka")
        anilist.recsystem.Upload2Kafka.upload2KafkaMediaInfo()
    }    
    "upload user info" should "not fail" in {
        val spark = {
            SparkSession
                .builder()
                .config("spark.master", "local[1]")
                .getOrCreate()
            }
        spark.conf.set("spark.conf_dir.kafka.location","/home/gazavat/git/AnimeRecService/kafka")
        anilist.recsystem.Upload2Kafka.upload2KafkaUserInfo()
    } 
}