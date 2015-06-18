package com.pushtechnology.benchmarks.clients;


import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.pushtechnology.benchmarks.experiments.CommonExperimentSettings;
import com.pushtechnology.benchmarks.monitoring.ExperimentCounters;
import com.pushtechnology.benchmarks.publishers.PingClientSendPublisher;
import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.message.MessageException;
import com.pushtechnology.diffusion.api.message.TopicMessage;

/**
 * A ping latency experiment client. Sends a ping every time it receives a ping.
 * 
 * @author nitsanw
 */
public final class PingClient extends LatencyMonitoringClient {
    /** message size. */
    private final int size;

    /** ping exchange topic. */
    private final String pingTopic;
    /** sent time. */
    private long sentTimeNanos;
    
    private int currentQueueSize;
    private int largestQueueSize;
    private int maximumQueueSize;

	private byte[] bytes;
	
	private final ScheduledThreadPoolExecutor sched;
	   
	private long delay;

    /**
     * @param experimentCountersP ...
     * @param sizeP message size
     * @param pingTopicP the topic on which we ping
     */
    public PingClient(ExperimentCounters experimentCountersP, int sizeP,
    		CommonExperimentSettings settings,
            String pingTopicP,
            ScheduledThreadPoolExecutor sched) {
        super(experimentCountersP, true,
        		settings,
                pingTopicP);
        this.size = sizeP;
        this.pingTopic = pingTopicP;
        this.delay = Long.parseLong(System.getProperty("ping.delay", "439")); // avoid lock-step
        
        this.bytes =new byte[size];
        
        this.sched = sched;
    }


    @Override
    public void afterMessage(ServerConnection serverConnection,
            TopicMessage topicMessage) {
    	
    	if (topicMessage.isDelta() && topicMessage.getTopicName().equals(PingClientSendPublisher.ROOT_TOPIC)) {
        	
            	// PingClientSendPublisher queue data
        	try {
				currentQueueSize = Integer.parseInt( topicMessage.getHeader(0).split(":")[1] );
        	largestQueueSize = Integer.parseInt( topicMessage.getHeader(1).split(":")[1] ); //client.getLargestQueueSize();
        	maximumQueueSize = Integer.parseInt( topicMessage.getHeader(2).split(":")[1] ); //client.getMaximumQueueSize();
        	
        	experimentCounters.sampleClientQueueSize(currentQueueSize);
        	experimentCounters.sampleClientQueueSizeHighWatermark(largestQueueSize);

        	//if(currentQueueSize>100)
        	//	System.out.println(serverConnection.getClientID()+" "+currentQueueSize+" "+largestQueueSize+" "+maximumQueueSize);
			} catch (Exception e) {
				e.printStackTrace();
			} //client.getCurrentQueueSize();
        	
            ping(topicMessage);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void afterServerConnect(ServerConnection serverConnection) {

            try {
                TopicMessage m = connection.createDeltaMessage(pingTopic, size);
				m.put(bytes);
		        ping(m);
			} catch (MessageException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    }

    /**
     * send a ping to server.
     * @param topicMessage 
     */
    @SuppressWarnings("deprecation")
    void ping(final TopicMessage topicMessage) {
        	
    	
    	if(sched != null){
        	sched.schedule(new Runnable() {
        	       public void run() { 
        	    	   sentTimeNanos = System.nanoTime();
                       try {
						connection.send(topicMessage);
					} catch (APIException e) {
						e.printStackTrace();
					}
        	       }
        	     }, this.delay, TimeUnit.MILLISECONDS);
        	
    	} else {
	    	sentTimeNanos = System.nanoTime();
            try {
				connection.send(topicMessage);
			} catch (APIException e) {
				e.printStackTrace();
			}
    	}
        	
          
    }
    @Override
    protected long getSentTimestamp(TopicMessage topicMessage) {
        return sentTimeNanos;
    }


    @Override
    protected long getArrivedTimestamp() {
        return System.nanoTime();
    }
}
