package anilist.recsystem

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should._
import anilist.recsystem.Utils._

class Upload2KafkaTest extends AnyFlatSpec with Matchers {
    "upload media info" should "not fail" in {
        spark
        spark.conf.set("spark.conf_dir.kafka.location","/home/gazavat/git/AnimeRecService/kafka")
        spark.conf.set("spark.conf_dir.postgres.location","/home/gazavat/git/AnimeRecService/postgres")
        anilist.recsystem.Upload2Kafka.upload2KafkaMediaInfo()
    }    
    "upload user info" should "not fail" in {
        spark
        spark.conf.set("spark.conf_dir.kafka.location","/home/gazavat/git/AnimeRecService/kafka")
        spark.conf.set("spark.conf_dir.postgres.location","/home/gazavat/git/AnimeRecService/postgres")
        anilist.recsystem.Upload2Kafka.upload2KafkaUserInfo()
    } 
}