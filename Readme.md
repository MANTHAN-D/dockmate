# Download and unzip Dockmate

- Download the source code in form of dockerMate.zip and unzip its content in a new directory (e.g. in /Users/Dockmate/dockerCompose)

# Build images of application specific containers

Images of application specific containers can be built using build-all.sh

```
cd /Users/Dockmate/dockerCompose
./build-all.sh
```

# Start Docker containers

Start all docker containers using below command.
In static configuration, along with rest of the setup (Haproxy load balancer, consul, consul-template, registrar containers) TWO RESTful web service containers will be started.

```
docker-compose up -d
```

## Testing Dockmate for Reactive Approach

Scale OUT : The current threshold setup for CPU usage is 25%. With any of the containers exceeding the CPU usage more than this threshold, the Elastic controller would send a request to spin another RESTful web service container to Provisioning System.

Scale IN : In case of idle situations, when there is not much load or request to be fulfilled by the cloud, if there are multiple idle service containers, Elastic controller would send a request to stop and remove unused RESTful web service containers to Provisioning System.


Run below command to test Reactive Approach
```
./runReactive.sh
```

Using apache benchmarking, with concurrency level as 20, load could be generated for a computationally intensive operation exposed at 'runReactive' endpoint. These requests would be sent to Docker-host which is also hosting HAProxy load balancer.

This command could be run for configurable duration (e.g. in below command it's run for 12000 seconds)

```
ab -kvr -c 20  -t 12000 http://DOCKER_HOST/cmpe281group4/runReactive
```

While load is generated for computationally intensive endpoint, the docker stats could be accessed using below URLs in browser

Access HAProxy Stat
```
http://DOCKER_HOST/stat
```

Access cAdvisor Dashboard
```
http://DOCKER_HOST:9091
```

Access Grafana Dashboard
```
http://DOCKER_HOST:3001
```

Logs could be monitored with this command

```
docker-compose logs -f | grep monitor
```

## Testing Dockmate for Proactive Approach
```
./runProactive2.sh
```

Using jmeter script (Sample1.jmx), generate load at any of the following endpoints:

1. http://DOCKER_HOST/cmpe281group4/
2. http://DOCKER_HOST/cmpe281group4/employees/
3. http://DOCKER_HOST/cmpe281group4/employees/1

These requests would be sent to RESTFul node application via HAProxy load-balancer.

While the request load is being generated, statistics of request_rate prediction can be visualized on following dashboard.
```
http://DOCKER_HOST:3000
```
Logs could be monitored with this command

```
docker-compose logs -f | grep proactive
```
