package com.dockmate;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.DockerCmdExecFactory;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.core.async.ResultCallbackTemplate;
import com.github.dockerjava.jaxrs.JerseyDockerCmdExecFactory;
import com.google.common.base.Optional;
import com.google.common.io.BaseEncoding;
import com.google.common.net.HostAndPort;
import com.orbitz.consul.Consul;
import com.orbitz.consul.KeyValueClient;
import com.orbitz.consul.async.ConsulResponseCallback;
import com.orbitz.consul.model.ConsulResponse;
import com.orbitz.consul.model.kv.Value;
import com.orbitz.consul.option.QueryOptions;
import org.fusesource.jansi.Ansi;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Dockmate : Reactive approach for Scaling Out and Scaling In
 * Implementation of scaling out and in based on CPU usage per containers
 * Created by rbathej on 11/6/16.
 */
public class DockerStatsApplication {

    // Constant and class variable declarations
    public static final String RUN_REACTIVE = "run_reactive";
    Logger log = Logger.getLogger(this.getClass().getName());

    Boolean local;
    String dockerHost = "tcp://192.168.99.100:2376";
    String dockerSocket = "unix:///var/run/docker.sock";
    String certPath = "/Users/rbathej/.docker/machine/machines/default";
    DockerClient dockerClient;
    private final Integer minInstances = 2;
    private final Integer maxInstances = 10;
    private Double maxLoadAllowed = new Double(25.0);
    private Double minLoadAllowed = new Double(10.0);

    private Map<String, Object> previousTotalUsage = new HashMap<>();
    private Map<String, Object> previousSystemUsage = new HashMap<>();
    public static Map<String, Object> percentageCPUUsage = new HashMap<>();

    Boolean doScaling = true;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

    /**
     * Constructor Declaration
     * @param local Set the flag to run either on local or on (Remote) GCE instance
     */
    public DockerStatsApplication(Boolean local) {
        this.local = local;
        DefaultDockerClientConfig.Builder builder = DefaultDockerClientConfig.createDefaultConfigBuilder();
        if (local) {
            builder.withDockerHost(dockerHost)
                    .withDockerTlsVerify(true)
                    .withDockerCertPath(certPath);
        }
        DockerClientConfig config = builder.build();
        DockerCmdExecFactory dockerCmdExecFactory = new JerseyDockerCmdExecFactory().withReadTimeout(1000).
                withConnectTimeout(1000).withMaxTotalConnections(100).withMaxPerRouteConnections(10);

        dockerClient = DockerClientBuilder.getInstance(config).withDockerCmdExecFactory(dockerCmdExecFactory).build();
    }

    /**
     * Main method to run the monitoring agent
     * @param args
     */
    public static void main(String args[]) {
        Boolean local = false; //Switch to true for local setup
        if (args.length > 0) {
            local = Boolean.valueOf(args[0]);
        }
        DockerStatsApplication monitoringApplication = new DockerStatsApplication(local);
        monitoringApplication.monitor();
    }


    /**
     * Method to start monitoring Web service Docker containers for their CPU Usage
     */
    private void monitor() {
        log.info("Starting monitoring " + (local ? "local" : "remote"));

        final Runnable monitoring = new Runnable() {
            AtomicBoolean waitforScaling = new AtomicBoolean(false);
            Integer numInstance = 0;

            @Override
            public void run() {
                List<String> recentlyAdded = new LinkedList<>();

                //Wait for scaling before pooling next one
                if (!waitforScaling.get()) {
                    List<String> wsContainers = getWSContainers(dockerClient);
                    if(wsContainers != null && wsContainers.size() > 0) {
                        numInstance = wsContainers.size();
                        log.info(Ansi.ansi().fgGreen().toString() + "--------------------------------------------------------------");
                        log.info(Ansi.ansi().fgGreen().toString() + "Currently running  " + numInstance + " web service containers.");
                        log.info(Ansi.ansi().fgGreen().toString() + "--------------------------------------------------------------");
                        log.info(Ansi.ansi().reset().toString());
                        if (numInstance < minInstances) {
                            scale(numInstance + 1, waitforScaling, "scaleOut");
                        }

                        List<Future<StatsCallbackResponse>> futureResponses = new ArrayList<>();
                        for (String wsContainer : wsContainers) {
                            if (!percentageCPUUsage.containsKey(wsContainer)) {
                                recentlyAdded.add(wsContainer);
                                previousSystemUsage.put(wsContainer,0L);
                                previousTotalUsage.put(wsContainer, 0L);
                            }
                        }
                        percentageCPUUsage.clear();
                        for (String wsContainer : wsContainers) {
                            Future<StatsCallbackResponse> statsCallbackResponseFuture = executor.submit(new StatsReporter(wsContainer));
                            futureResponses.add(statsCallbackResponseFuture);
                        }

                        for (Future<StatsCallbackResponse> futureResponse : futureResponses) {
                            try {
                                StatsCallbackResponse statsCallbackResponse = futureResponse.get();

                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            } catch (ExecutionException e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        log.info(Ansi.ansi().fgRed().toString() + "--------------------------------------------------------------");
                        log.info(Ansi.ansi().fgRed().toString() + "No container running at all!");
                        log.info(Ansi.ansi().fgBrightCyan().toString() + "Let us start failure recovery by starting first web service container again!");
                        log.info(Ansi.ansi().fgRed().toString() + "--------------------------------------------------------------");
                        log.info(Ansi.ansi().reset().toString());
                        scale(1, waitforScaling, "scaleOut");
                    }
                }


                //Plot these values in graph
                //CPUUsagePerContainer.plotGraphForCPUUsage();

                Boolean scaleOut = false;
                Boolean scaleIn = false;

                //Check if any of the containers' cpuUsage has exceeded the defined thresholds
                for (Map.Entry<String, Object> entry : percentageCPUUsage.entrySet()) {
                    if ((Double) entry.getValue() > maxLoadAllowed) {
                        scaleOut = true;
                        break;
                    }
                    if ((Double) entry.getValue() < minLoadAllowed && !recentlyAdded.contains(entry.getKey())) {
                        scaleIn = true;
                        break;
                    }
                }

                log.info("flag ScaleIn =  " + scaleIn);
                log.info("flag scaleOut =  " + scaleOut);

                log.info(Ansi.ansi().fgBrightCyan().toString() + "--------------------------------------------------------------");
                log.info(Ansi.ansi().fgBrightCyan().toString() + "Switch for run_reactive algorithm = " + doScaling);
                log.info(Ansi.ansi().fgBrightCyan().toString() + "percentCPUUsage per web service container = " + percentageCPUUsage);
                log.info(Ansi.ansi().fgBrightCyan().toString() + "--------------------------------------------------------------");
                log.info(Ansi.ansi().reset().toString());
                if (scaleOut && numInstance < maxInstances) {
                    scale(numInstance + 1, waitforScaling, "scaleOut");
                }
                if (scaleIn && numInstance > minInstances) {
                    scale(numInstance - 1, waitforScaling, "scaleIn");
                }

            }
        };


        final Runnable configChanges = new Runnable() {
            Consul consul = Consul.builder().withHostAndPort(HostAndPort.fromParts("consul", 8500)).build();
            final KeyValueClient keyValueClient = consul.keyValueClient();

            @Override
            public void run() {
                ConsulResponseCallback<Optional<Value>> callback = new ConsulResponseCallback<Optional<Value>>() {

                    AtomicReference<BigInteger> index = new AtomicReference<>(null);

                    @Override
                    public void onComplete(ConsulResponse<Optional<Value>> consulResponse) {

                        try {
                            if (consulResponse.getResponse().isPresent()) {
                                Value v = consulResponse.getResponse().get();
                                String runReactive = new String(BaseEncoding.base64().decode(v.getValue().get()));
                                log.info("Value is: " + runReactive + " with index " + consulResponse.getIndex());
                                doScaling = Boolean.valueOf(runReactive);
                            }
                        } catch (Exception e) {
                            log.severe(e.getMessage());
                        }
                        index.set(consulResponse.getIndex());
                        watch();
                        // curl http://consul:8500/v1/kv/run_reactive
                        // curl -X PUT -d'false' http://consul:8500/v1/kv/run_reactive
                    }

                    void watch() {
                        keyValueClient.getValue(RUN_REACTIVE, QueryOptions.blockMinutes(1, index.get()).build(), this);
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        watch();
                    }
                };
                keyValueClient.getValue(RUN_REACTIVE, QueryOptions.blockMinutes(1, new BigInteger("0")).build(), callback);
                log.info("Started watching for configuration changes");
            }
        };

        final ScheduledFuture<?> mon = scheduler.scheduleAtFixedRate(monitoring, 0, 15, SECONDS);
        final ScheduledFuture<?> configChangeF = scheduler.schedule(configChanges, 1, MILLISECONDS);
    }


    /**
     * Method to Scale out or Scale in the web service docker containers
     * @param numInstances
     * @param waitforScaling
     * @param scaleOut
     */
    private void scale(Integer numInstances, AtomicBoolean waitforScaling, String scaleOut) {
        if (doScaling) {
            if (numInstances != 1 && (numInstances < minInstances || numInstances > maxInstances)) {
                log.info(Ansi.ansi().fgRed().toString() + "Scale out/in than min/max instances not allowed");
                log.info(Ansi.ansi().fgRed().toString() + "No more DockMate container can be started!!");
                log.info(Ansi.ansi().reset().toString());
                waitforScaling.set(true);
            } else {
                log.info(Ansi.ansi().fgBrightYellow().toString() + "--------------------------------------------------------------");
                if(scaleOut.equalsIgnoreCase("scaleOut"))
                    log.info(Ansi.ansi().fgBrightYellow().toString() + "Scaling OUT to " + numInstances + " web service containers");
                else if(scaleOut.equalsIgnoreCase("scaleIn"))
                    log.info(Ansi.ansi().fgBrightYellow().toString() + "Scaling IN to " + numInstances + " web service containers");
                log.info(Ansi.ansi().fgBrightYellow().toString() + "--------------------------------------------------------------");
                log.info(Ansi.ansi().reset().toString());
                String[] command = {"docker-compose", "scale", "ws=" + numInstances};
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(new File(local ? "/Users/rbathej/SJSU/3_281/dockMate/dockerCompose" : "/dockerCompose"));
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                Process p = null;
                try {
                    waitforScaling.set(true);
                    p = pb.start();
                    int exitVal = p.waitFor();
                    waitforScaling.set(false);
                    //log.info("Scaling of containers exited with status " + exitVal);
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    /**
     * Method to get active web service docker containers running in Docker host
     * @param dockerClient
     * @return
     */
    private List<String> getWSContainers(DockerClient dockerClient) {
        List<Container> containers = dockerClient.listContainersCmd().exec();
        List<String> wsContainers = new LinkedList<String>();
        for (Container c : containers) {
            if (c.getImage().equalsIgnoreCase("myws")) {
                Collections.addAll(wsContainers, c.getNames());
            }
        }
        return wsContainers;
    }


    /**
     * A callback method to get the Docker Stats and determine the CPU usage per container
     */
    private class StatsCallbackResponse extends ResultCallbackTemplate<StatsCallbackResponse, Statistics> {
        private final String container;
        private final CountDownLatch countDownLatch;

        private Boolean gotStats = false;

        public StatsCallbackResponse(CountDownLatch countDownLatch, String container) {
            this.container = container;
            this.countDownLatch = countDownLatch;
        }

        public void onNext(Statistics stats) {
            if (stats != null) {
                gotStats = true;
                //log.info("Status for container " + container + " is " + stats.getCpuStats());
                Long prevTotalUsage = null, currTotalUsage = null;
                Long currSystemUsage = null, prevSystemUsage = null;
                Double percUsage;
                // Get only CPU Stats
                for (Map.Entry<String, Object> entry : stats.getCpuStats().entrySet()) {

                    if (entry.getKey().equals("cpu_usage")) {
                        HashMap<String, Object> cpuStats = (HashMap<String, Object>) entry.getValue();
                        for (Map.Entry<String, Object> cpuStatEntry : cpuStats.entrySet()) {
                            if (cpuStatEntry.getKey().equals("total_usage")) {
                                currTotalUsage = (Long) cpuStatEntry.getValue();
                                if (previousTotalUsage.get(container) == null) {
                                    previousTotalUsage.put(container, currTotalUsage);
                                    prevTotalUsage = 0L;
                                } else {
                                    prevTotalUsage = (Long) previousTotalUsage.get(container);
                                    previousTotalUsage.put(container, currTotalUsage);
                                }
                            }
                        }
                    }

                    if (entry.getKey().equals("system_cpu_usage")) {
                        if (previousSystemUsage.get(container) == null) {
                            previousSystemUsage.put(container, entry.getValue());
                            prevSystemUsage = 0L;
                        } else {
                            prevSystemUsage = (Long) previousSystemUsage.get(container);
                            previousSystemUsage.put(container, entry.getValue());
                        }
                        currSystemUsage = (Long) entry.getValue();
                    }
                }

                // calculate the percentageCPU usage here
                /*log.info("Calculating the percentage CPU usage for " + container + " with these values: \n" +
                        "\tcurrTotalUsage : " + currTotalUsage + "\n" +
                        "\tprevTotalUsage : " + prevTotalUsage + "\n" +
                        "\tcurrSystemUsage : " + currSystemUsage + "\n" +
                        "\tprevSystemUsage : " + prevSystemUsage);
                */
                percUsage = Double.valueOf(((currTotalUsage.longValue() - prevTotalUsage.longValue()) * 100) / (currSystemUsage.longValue() - prevSystemUsage.longValue()));
                percentageCPUUsage.put(container, percUsage);

            }
            //log.info("\n~~~~~~~~~~~~~~~~~~~~Load for  : " + container + " = " + percentageCPUUsage.get(container));
            countDownLatch.countDown();
        }

        public Boolean gotStats() {
            return gotStats;
        }
    }


    /**
     * An inner class to run the docker stats java API
     */
    class StatsReporter implements Callable<StatsCallbackResponse> {

        private StatsCallbackResponse statsCallbackResponse;
        private String wsContainer;

        public StatsReporter() {
        }

        public StatsReporter(String wsContainer) {
            this.wsContainer = wsContainer;
        }

        @Override
        public StatsCallbackResponse call() throws Exception {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            statsCallbackResponse = dockerClient.statsCmd(wsContainer).exec(new StatsCallbackResponse(countDownLatch, wsContainer));

            try {
                countDownLatch.await(3, TimeUnit.SECONDS);
                statsCallbackResponse.close();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return statsCallbackResponse;
        }
    }
}
