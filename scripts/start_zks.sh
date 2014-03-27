#!/bin/bash

docker run -d --name zk1 edpaget/zookeeper:3.4.6 \
  -i 0 -c zk1.zookeeper.dev.docker,zk2.zookeeper.dev.docker,zk3.zookeeper.dev.docker

docker run -d --name zk2 edpaget/zookeeper:3.4.6 \
  -i 1 -c zk1.zookeeper.dev.docker,zk2.zookeeper.dev.docker,zk3.zookeeper.dev.docker

docker run -d --name zk3 edpaget/zookeeper:3.4.6 \
  -i 2 -c zk1.zookeeper.dev.docker,zk2.zookeeper.dev.docker,zk3.zookeeper.dev.docker
