# DOCKER-VERSION 0.9.0
# VERSION 0.3

FROM ubuntu:12.04
MAINTAINER Edward Paget <ed@zooniverse.org>

RUN apt-get update 

RUN DEBIAN_FRONTEND=noninteractive apt-get install -y -q openjdk-7-jre-headless supervisor
RUN mkdir -p /opt/zoo-event-1.0.0-SNAPSHOT
ADD supervisord.conf /etc/supervisor/conf.d/supervisord.conf
ADD conf/conf-prod.edn /opt/zoo-event-1.0.0-SNAPSHOT/conf.edn
ADD target/zoo-event-1.0.0-SNAPSHOT-standalone.jar /opt/zoo-event-1.0.0-SNAPSHOT/zoo-event.jar

EXPOSE 8080

CMD ["/usr/bin/supervisord"]
