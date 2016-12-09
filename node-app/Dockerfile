FROM node:argon

# Create app directory
RUN mkdir -p /usr/src/app1
WORKDIR /usr/src/app1

# Install app dependencies
COPY package.json /usr/src/app1/
RUN npm install

# Bundle app source
COPY . /usr/src/app1

EXPOSE 80
CMD [ "npm", "start" ]
