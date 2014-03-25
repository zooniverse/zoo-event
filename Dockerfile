# DOCKER-VERSION 0.9.0
# VERSION 0.2

FROM ubuntu:12.04
MAINTAINER Edward Paget <ed@zooniverse.org>

RUN echo "deb http://archive.ubuntu.com/ubuntu precise main universe" > /etc/apt/sources.list
RUN apt-get update 
RUN apt-get upgrade -y

RUN apt-get install -y -q openjdk-7-jre-headless
RUN mkdir -p /opt/zoo-events
ADD conf-docker.edn /opt/zoo-events/conf.edn
ADD target/zoo-live-0.3.0-SNAPSHOT-standalone.jar /opt/zoo-events/zoo-events-0.3.0.jar

EXPOSE 8080

#ENTRYPOINT ["java", "-jar", "/opt/zoo-events/zoo-events-0.3.0.jar", "/opt/zoo-events/conf.edn"]
