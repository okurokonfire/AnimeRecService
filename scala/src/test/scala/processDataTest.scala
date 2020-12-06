package anilist.recsystem

import  org.scalatest.flatspec.AnyFlatSpec
import  org.scalatest.matchers.should._

import anilist.recsystem.Utils._
import anilist.recsystem.ProcessData._
import org.apache.spark.sql.functions._

class ProcessDataTest extends AnyFlatSpec with Matchers {
    // "process user" should "not fail" in {
    //     anilist.recsystem.ProcessData.processMediaInfo(anilist.recsystem.CollectJsonInfo.collectMediaInfo(1))
    // }

    "process media" should "not fail" in {
        spark
        spark.conf.set("spark.conf_dir.postgres.location","/home/gazavat/git/AnimeRecService/postgres")
        processMediaInfo(anilist.recsystem.CollectJsonInfo.collectMediaInfo(5))
    }

    "batch process media" should "not fail" in {
        spark
        val kafkaParams = Map("kafka.bootstrap.servers"->"localhost:9092", "subscribe" -> "test_topic","startingOffsets"->"earliest")
        spark.conf.set("spark.conf_dir.postgres.location","/home/gazavat/git/AnimeRecService/postgres")
        spark.read.format("kafka").options(kafkaParams).load.select(col("value").cast("string")).collect().map(x => x.getString(0)).map(x => processMediaInfo(x))
    }

    "process user" should "not fail" in {
        spark
        spark.conf.set("spark.conf_dir.postgres.location","/home/gazavat/git/AnimeRecService/postgres")
        val kafkaParams = Map("kafka.bootstrap.servers"->"localhost:9092", "subscribe" -> "test_user_topic","startingOffsets"->"earliest")
        val first = spark.read.format("kafka").options(kafkaParams).load.select(col("value").cast("string")).collect().map(x => x.getString(0)).head
        processUserInfo(first)
    }

    "batch process user" should "not fail" in {
        spark
        val kafkaParams = Map("kafka.bootstrap.servers"->"localhost:9092", "subscribe" -> "test_user_topic","startingOffsets"->"earliest")
        spark.conf.set("spark.conf_dir.postgres.location","/home/gazavat/git/AnimeRecService/postgres")
        spark.read.format("kafka").options(kafkaParams).load.select(col("value").cast("string")).collect().map(x => x.getString(0)).map(x => processUserInfo(x))
        spark.stop
    }
}