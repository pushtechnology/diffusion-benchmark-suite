/*
 * Copyright 2013 Push Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package clients;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.evpub.EventPublisherConnection;
import com.pushtechnology.diffusion.api.evpub.EventPublisherListener;
import com.pushtechnology.diffusion.api.message.FragmentedMessageLifecycle;
import com.pushtechnology.diffusion.api.message.MessageException;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.threads.RunnableTask;
import com.pushtechnology.diffusion.api.threads.ThreadService;

/**
 * eventPub:
 * connection config: buff sizes, queue size, timeout
 * message config: size distribution(1*1K,3*200b,5*100b), fragmentation(size,delay), freq
 * topic tree: number of topics, levels, add/remove freq/proportion
 * 
 * @author nwakart
 *
 */
public class TopicChurnEventPublisher implements EventPublisherListener {

    // Topic settings
    private static final int topics = 1000000;
    private static final int subTopicLayers = 3; 
    
    // Connection settings
    private static final int connectionBufferSize = 32 * 1024;
    private static final int connectionTimeout = 10*1000;
    private static final int messageQueueSize = 1000000;
    
    // Message settings
    private static final int messageFrequency = 500;
    private static final int messageSize = 600;
    private static final int messageCapacity = 1024;
    private static final int messageSizeRange = 100;
    
    private static final int fragementSize = 6 * 1024;
    
    private volatile EventPublisherConnection evpubConnection;

    private static AtomicLong maxActionTime = new AtomicLong();
    private final List<List<String>> layerTopicsList = new ArrayList<List<String>>(subTopicLayers);
    private final AtomicLong[] layerCounters = new AtomicLong[subTopicLayers];
    private final ScheduledExecutorService timer = Executors.newSingleThreadScheduledExecutor();

    /**
     * 
     * Create a connection
     *
     * @return
     * @throws APIException
     * @since n.n
     */
    public EventPublisherConnection createConnection() throws APIException {
        EventPublisherConnection theConnection = new EventPublisherConnection("localhost",3098,this);
        theConnection.getServerDetails().setOutputBufferSize(connectionBufferSize);
        theConnection.getServerDetails().setInputBufferSize(connectionBufferSize);
        theConnection.getServerDetails().setConnectionTimeout(connectionTimeout);
        theConnection.setMessageQueueSize(messageQueueSize);
        theConnection.connect();
        return theConnection;
    }
    
    public TopicChurnEventPublisher() throws APIException, IOException {
        for(int i=0;i<subTopicLayers;i++){
            layerCounters[i] = new AtomicLong(0L);
            layerTopicsList.add(new ArrayList<String>());
        }
        
        evpubConnection = createConnection();
        
        removeTopic("E");
        createTopic("E/E_1/E");
        for (int i = 0;i<topics;i++) {
            if(i % 10 == 0)
                createTopic(0);
            else if(i%10 < 3)
                createTopic(1);
            else
                createTopic(2);
        }
        timer.scheduleAtFixedRate(new Runnable() {
            Random random = new Random();
            @Override
            public void run() {
                try {
                    for (int i = 0;i<50;i++) {
                        if(random.nextFloat() <= 0.01){
                            if(random.nextBoolean()){
                                createTopic(0);
                                for (int j = 0;j<10;j++) {
                                    createTopic(1);
                                    for (int z = 0;z<2;z++) {
                                        createTopic(2);
                                    }
                                }
                            }else{
                                removeTopic(random,0);
                                continue;
                            }
                        }
                        for (int j = 0;j<10;j++) {
                            if(random.nextFloat() <= 0.05){
                                if(random.nextBoolean()){
                                    createTopic(1);
                                    for (int z = 0;z<2;z++) {
                                        createTopic(2);
                                    }
                                }else{
                                    removeTopic(random,1);
                                    continue;
                                }
                            }
                            for (int z = 0;z<2;z++) {
                                if(random.nextFloat() <= 0.1){
                                    if(random.nextBoolean()){
                                        createTopic(2);
                                    }else{
                                        removeTopic(random,2);
                                    }
                                }
                            }
                        }
                    }
                }
                catch (APIException e) {
                    e.printStackTrace();
                }
            }
        },1000,1000,TimeUnit.MILLISECONDS);
        timer.scheduleAtFixedRate(new Runnable() {
            Random random = new Random();
            @Override
            public void run() {
                try {
                    for (int i = 0;i<10;i++) {
                        publishTopic(random,2);
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                
            }
        },10,100,TimeUnit.MILLISECONDS);
                
        // Start threads sending small messages
        ThreadService.schedule(new RunnableTask(){
            @Override
            public void run() {
                try {
                    publish(messageSize,messageSizeRange,messageCapacity);
                }
                catch (APIException e)
                {
                    Logs.warning("Message",e);
                }
            }
        },5,messageFrequency,TimeUnit.MILLISECONDS,false);
    }
    
    private void removeTopic(Random randy,int layer) throws MessageException, APIException{
        String remove = layerTopicsList.get(layer).remove(randy.nextInt(layerTopicsList.get(layer).size()));
        if(layer < 1){
            Iterator<String> iterator = layerTopicsList.get(1).iterator();
            while (iterator.hasNext()){
                if(iterator.next().startsWith(remove))
                    iterator.remove();
            }
        }
        if(layer < 2){
            Iterator<String> iterator = layerTopicsList.get(2).iterator();
            while (iterator.hasNext()){
                if(iterator.next().startsWith(remove))
                    iterator.remove();
            }
        }
        removeTopic(remove);
    }
    private void publishTopic(Random randy,int layer) throws MessageException, APIException{
        List<String> layerTopics = layerTopicsList.get(layer);
        if(layerTopics.size() > 0){
            String topic = layerTopics.get(randy.nextInt(layerTopics.size()));
            TopicMessage message = evpubConnection.createDeltaMessage("ROOT/DATA/" +topic,20);
            message.put(new byte[500]);
            evpubConnection.send(message);
        }
    }

    /**
     * Send a message of a given size, variation, capacity
     * @param size
     * @param range
     * @param capacity
     * @throws APIException
     * @since n.n
     */
    private void publish(int size, int range, int capacity) throws APIException {
        // Pick a connection to use
        publish("ROOT/DATA",size,range,capacity);
    }
    
    /**
     * Send a message of a given size, variation, capacity, topic and connection
     * @param topic
     * @param connectionIndex
     * @param size
     * @param range
     * @param capacity
     * @throws APIException
     * @since n.n
     */
    private void publish(String topic,int size, int range, int capacity) throws APIException {
        TopicMessage message;
        message = evpubConnection.createDeltaMessage(topic,capacity);
        message.setFragmentSize(fragementSize);
        message.setFragmentedMessageLifecycle(new FragmentedMessageLifecycle(50));
        byte[] bytes = generateBytes(size,range);
        message.put(bytes);
        evpubConnection.send(message);
    }
    
    /**
     * Get the bytes to send
     * @param size
     * @param range
     * @return
     * @since n.n
     */
    private byte[] generateBytes(int size, int range) {
        Random random = new Random();
        int targetMessageSize = size + (range - random.nextInt(2*range));
        byte[] bytes = new byte[targetMessageSize];
        return bytes;
    }
    
    private void createTopic(int layer) throws MessageException, APIException {
        layerCounters[layer].incrementAndGet();
        String topic = "E/E_1/E/E_" + layerCounters[0].get();
        if(layer > 0){
            topic += "/"+layerCounters[1].get();
        }
        if(layer > 1){
            topic += "/"+layerCounters[2].get();
        }
        layerTopicsList.get(layer).add(topic);
        createTopic(topic);
    }

    protected void createTopic(String topic) throws APIException,
    MessageException {
        TopicMessage message = evpubConnection.createDeltaMessage("ROOT/META",20);
        message.putFields("ADD",topic,"load data");
        evpubConnection.send(message);
        for(int i=0;i<100;i++)
            Thread.yield();
    }

    protected void removeTopic(String topic) throws APIException,
    MessageException {
        TopicMessage message = evpubConnection.createDeltaMessage("ROOT/META",20);
        message.putFields("REMOVE",topic);
        evpubConnection.send(message);
        LockSupport.parkNanos(TimeUnit.MICROSECONDS.toNanos(100));
    }

    public void messageFromServer(
    EventPublisherConnection connection,TopicMessage message) {
        try {
            long newValue = Long.parseLong(message.asFields().get(1)) ;
            if(newValue > maxActionTime.get())
                maxActionTime.set(newValue);
        }
        catch (MessageException e) {
            e.printStackTrace();
        }
    }

    public void serverDisconnected(EventPublisherConnection connection) {
        System.out.println(
            "Event Publisher Connection Closed "+connection);
        try {
            connection = createConnection();
        }
        catch (APIException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        new TopicChurnEventPublisher();
    }

    
}
