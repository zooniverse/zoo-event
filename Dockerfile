# DOCKER-VERSION 0.9.0
# VERSION 0.3

FROM ubuntu:12.04
MAINTAINER Edward Paget <ed@zooniverse.org>

RUN apt-get update 

RUN DEBIAN_FRONTEND=noninteractive apt-get install -y -q openjdk-7-jre-headless supervisor
RUN mkdir -p /opt/zoo-event
ADD supervisord.conf /etc/supervisor/conf.d/supervisord.conf
ADD target/zoo-event-2.0.2-standalone.jar /opt/zoo-event/zoo-event.jar

EXPOSE 8080

CMD ["/usr/bin/supervisord"]
