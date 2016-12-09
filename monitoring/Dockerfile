FROM ubuntu

MAINTAINER RENU BATHEJA

# Installation:
# Install docker, docker-compose, jre and curl as prerequisites 
# for running monitoring agent container

RUN apt-get update && \
   apt-get install -y curl
RUN curl -L "https://github.com/docker/compose/releases/download/1.8.1/docker-compose-$(uname -s)-$(uname -m)" > /usr/local/bin/docker-compose

RUN chmod +x /usr/local/bin/docker-compose
RUN curl -sSL https://get.docker.com/ | sh
RUN apt-get install -y default-jre

# Set the dockerized entry-point

ENTRYPOINT ["/bin/sh"]
CMD ["/start.sh"]

# Copy dockMate jar to monitoring agent container

ADD DockMate/build/libs/dockMate-1.0-all.jar /dockMate-1.0-all.jar
ADD start.sh /start.sh
RUN chmod u+x /start.sh
