package com.pushtechnology.benchmarks.experiments;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.pushtechnology.benchmarks.clients.ExperimentClient;
import com.pushtechnology.benchmarks.clients.PingAndThroughputClient;
import com.pushtechnology.benchmarks.clients.PingClient;
import com.pushtechnology.benchmarks.publishers.PingClientSendPublisher;
import com.pushtechnology.benchmarks.util.Factory;

public class PingAndThroughputExperiment implements Runnable {
    /** the experiment loop. */
    private final ExperimentControlLoop loop;
	   private static final ScheduledThreadPoolExecutor sched = new ScheduledThreadPoolExecutor(2);

    /**
     * client connections to be closed on close of factory and queried for
     * latency stats.
     */
    private final Set<PingAndThroughputClient> clients = Collections
            .newSetFromMap(new ConcurrentHashMap<PingAndThroughputClient, Boolean>());

    /**
     * @param settings ...
     * 
     */
    public PingAndThroughputExperiment(CommonExperimentSettings settings) {
    	
    	///sched.prestartAllCoreThreads();
    	//sched.setCorePoolSize(1);
    	
        loop = new ExperimentControlLoop(settings) {
            @Override
            protected void wrapupAndReport() {
                // CHECKSTYLE:OFF
//                Histogram histogramSummary =
//                        new Histogram(TimeUnit.SECONDS.toNanos(10), 3);

            	this.getExperimentCounters().reportLatency(getOutput());
            	
//                for (PingClient connection : clients) {
//                    histogramSummary.add(connection.getHistogram());
//                }
//                histogramSummary.outputPercentileDistribution(getOutput(), 1, 1000.0);
                // CHECKSTYLE:ON
            }
        };
        loop.setClientFactory(new Factory<ExperimentClient>() {
        	
        	int clientId = 0;
        	
            @Override
            public ExperimentClient create() {
            	
            	// If the client is pinging
            	String pingTopicName = PingClientSendPublisher.ROOT_TOPIC;
            	if(loop.getClientSettings().isPingTopicSend()){
            		pingTopicName += "_"+clientId++;
            	}
            	PingAndThroughputClient pingClient =
                        new PingAndThroughputClient(loop.getExperimentCounters(),
                                loop.getClientSettings().getMessageSize(),
                                loop.getClientSettings(),
                                pingTopicName, loop.getClientSettings().getRootTopic(),
                                sched);
                clients.add(pingClient);
                return pingClient;
            }

            @Override
            public void close() {
                for (PingAndThroughputClient connection : clients) {
                    connection.disconnect();
                }
                sched.shutdownNow();
            }
        });
        ExperimentLoadStrategy defaultLoadStrategy =
                new DefaultLoadStrategy(loop.getClientSettings(),
                        loop.getExperimentCounters());
        loop.setLoadStartegy(defaultLoadStrategy);
    }

    @Override
    public void run() {
        loop.run();
    }
}
