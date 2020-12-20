package anilist.recsystem

//import play.api.libs.json._
import scala.util.Try
import org.apache.spark.sql.functions._
import anilist.recsystem.Utils._


object getFromKafka {
    //spark
    //spark.conf.set("spark.conf_dir.postgres.location","/home/gazavat/git/AnimeRecService/postgres")
    //spark.conf.set("spark.conf_dir.kafka.location","/home/gazavat/git/AnimeRecService/kafka")


    def getFromKafkaUserInfo() = {
        val kafkaParams = getUpdatedKafkaParams("kafka_params_get_user.conf")
        val sdf = spark.readStream.format("kafka").options(kafkaParams).load.select(col("value").cast("string"))

        val streamingDF = createSink("getUser", sdf) {
             (df, id) => 
            //println(df.count)
            //df.show(false)
            //println(s"This is batch $id")

            val entries = df.collect().map(x => x.getString(0))
            entries.map(x => anilist.recsystem.ProcessData.processUserInfo(x))
            
        }
        val startedStream = streamingDF.start
        startedStream.awaitTermination()
    }

    def getFromKafkaMediaInfo():Unit = {
        val kafkaParams = getUpdatedKafkaParams("kafka_params_get_media.conf")
        val sdf = spark.readStream.format("kafka").options(kafkaParams).load.select(col("value").cast("string"))

        val streamingDF = createSink("getMedia", sdf) {
             (df, id) => 
            //println(df.count)
            //df.show(false)
            //println(s"This is batch $id")

            val entries = df.collect().map(x => x.getString(0))
            entries.map(x => anilist.recsystem.ProcessData.processMediaInfo(x))
            
        }
        val startedStream = streamingDF.start
        startedStream.awaitTermination()
    }

    def main(args: Array[String]): Unit = {
        val mode = Try(args(0)).getOrElse(throw new Exception("you should declare mode"))
        mode match {
            case "media" => getFromKafkaMediaInfo()
            case "user"  => getFromKafkaUserInfo()
            case _       => throw new Exception("incorrect mode")
        }
    }
}