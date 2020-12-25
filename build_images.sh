#!/bin/bash
docker build -t okurokfire/upload_user_to_kafka -f dockerfiles/kafka_upload_user/Dockerfile .
docker build -t okurokfire/upload_media_to_kafka -f dockerfiles/kafka_upload_media/Dockerfile .
docker build -t okurokfire/get_user_from_kafka -f dockerfiles/process_user/Dockerfile .
docker build -t okurokfire/get_media_from_kafka -f dockerfiles/process_media/Dockerfile .
