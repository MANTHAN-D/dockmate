#!/bin/bash
# author RENU BATHEJA
# Script for buidling images of application specific containers

set -e

# build image for node application
docker build -t myws node-app/

# build image for haproxy service
docker build -t myproxy haproxy/

# build image for mongodb
docker build -t mymongodb mongodb/

# build image for reactive monitoring agent (using docker-stats API)
pushd monitoring/DockMate/
./gradlew shadowJar
popd
docker build -t monitoring monitoring

# build image for proactive monitoring agent and proactive algorithm container
docker build -t proactive_monitoring proactive_monitoring/
docker build -t proactivealgo proactivealgo/

# build image for Proactive Approach 1
docker build -t myproactive proactiveapp1/


# Run using 
# docker-compose up -d
# docker-compose logs -f | grep monitor 
