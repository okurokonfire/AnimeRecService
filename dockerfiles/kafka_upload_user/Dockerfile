FROM okurokfire/spark_ars_base
RUN mkdir /kafka
RUN mkdir /postgres
COPY scala/target/scala-2.12/ars_2.12-0.1.0.jar .
COPY postgres/post* /postgres/
COPY postgres/root.crt /postgres/
CMD ["spark-submit","--conf","spark.jars.ivy=/tmp/.ivy","--master","local[1]","--conf","spark.conf_dir.kafka.location=/kafka","--conf","spark.conf_dir.postgres.location=/postgres","--packages","org.apache.spark:spark-sql-kafka-0-10_2.12:3.0.1,org.postgresql:postgresql:9.4.1212,com.typesafe.play:play-json_2.12:2.8.1","--class","anilist.recsystem.Upload2Kafka","ars_2.12-0.1.0.jar","user"] 
COPY kafka/kafka_params_upload_user.conf /kafka/
#CMD ["spark-shell","-i", "try.scala"]
#COPY try.scala .
