FROM ubuntu
RUN apt-get update && \
   apt-get install -y bash netcat-openbsd bc curl jq default-jre wget

RUN curl -L "https://github.com/docker/compose/releases/download/1.8.1/docker-compose-$(uname -s)-$(uname -m)" > /usr/local/bin/docker-compose
RUN chmod +x /usr/local/bin/docker-compose
RUN curl -sSL https://get.docker.com/ | sh

RUN apt-get install -y nodejs
RUN apt-get install -y npm

RUN rm /bin/sh && ln -s /bin/bash /bin/sh

RUN mkdir /logs
VOLUME ["/logs"]

#Setup Node application for Proactive approach
# Create app directory
RUN mkdir -p /usr/src/proactive-app
WORKDIR /usr/src/proactive-app

# Install app dependencies
COPY package.json /usr/src/proactive-app/
RUN npm install

# Bundle app source
COPY . /usr/src/proactive-app

EXPOSE 3000
CMD [ "nodejs", "bin/www", "app.js" ]

ADD scaler.jar /scaler.jar