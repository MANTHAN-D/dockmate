package com.dockmate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Dockmate : Plotting of CPU Usage per container in a graph
 * Created by rbathej on 11/21/16.
 */
public class CPUUsagePerContainer {
    private static Timer timer = new Timer();

    public static void plotGraphForCPUUsage() {
        timer.scheduleAtFixedRate(
                new TimerTask()
                {
                    public void run()
                    {
                        if(DockerStatsApplication.percentageCPUUsage.size() > 0) {
                            plotGraphForContainers(DockerStatsApplication.percentageCPUUsage);
                        }
                    }
                },
                0,      // run first occurrence immediately
                1000);  // run every 1 seconds*/
    }


    /**
     * A static method to plot the cpu usage per containers in a graph
     * @param percentageCPUUsageForGraph
     */
    private static void plotGraphForContainers(Map<String, Object> percentageCPUUsageForGraph) {
        String queryParams = constructQueryParams(percentageCPUUsageForGraph);
        URL url = null;
        try {
            url = new URL("http://192.168.99.100:8081/cpu_usage?" + queryParams);
            BufferedReader br = new BufferedReader(new InputStreamReader(url.openStream()));
            String strTemp = "";
            while (null != (strTemp = br.readLine())) {
                //System.out.println(strTemp);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * A static method to construct the query params for graph plotting API
     * @param percentageCPUUsageForGraph
     * @return
     */
    private static String constructQueryParams(Map<String, Object> percentageCPUUsageForGraph) {
        String queryParams = "";
        int i = 1;
        for (Map.Entry entry : percentageCPUUsageForGraph.entrySet()) {
            queryParams += "container" + i + "=" + (Double) entry.getValue() + "&";
            i++;
        }
        return queryParams;
    }

}
