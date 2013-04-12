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
package publishers;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.message.FragmentedMessageLifecycle;
import com.pushtechnology.diffusion.api.message.MessageException;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.publisher.Client;
import com.pushtechnology.diffusion.api.publisher.EventConnection;
import com.pushtechnology.diffusion.api.publisher.Publisher;
import com.pushtechnology.diffusion.api.topic.Topic;

public final class EventPubDrivenPublisher extends Publisher {
	public static final String META_TOPIC = "ROOT/META";
	public static final String DATA_TOPIC = "ROOT/DATA";
	public static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat(
			"HH:mm:ss.SSS");
	private Topic dataTopic;
	// initiailized on initialLoad
	private ExecutorService asyncCommandExecuter;
	private Timer timer;
	private final AtomicLong subscribeCounter = new AtomicLong();
	private final AtomicLong unsubscribeCounter = new AtomicLong();
	private final AtomicLong addCost = new AtomicLong();
	private final AtomicLong removeCost = new AtomicLong();
	private final AtomicLong publishCost = new AtomicLong();
	private final AtomicLong addCounter = new AtomicLong();
	private final AtomicLong removeCounter = new AtomicLong();
	private final AtomicLong publishCounter = new AtomicLong();

	private int loadMessageSize = 100;
	private int fragmentSize = 10*1024;
	private int fragmentThreshold = 20*1024;
	private int fragmentDelay = 100;
	private int clientQueueSize = 100000;
	private int cpuBurnPerPublish = 10;
	private int cpuSleepPerPublish = 10;
	@Override
	protected void initialLoad() throws APIException {
		Logs.info(getPublisherName() + "initialLoad");
		addTopic(META_TOPIC);
		dataTopic = addTopic(DATA_TOPIC);
		dataTopic.setAutoSubscribe(true);
		int threads = getProperty("publisherThreads", 0);
		loadMessageSize = getProperty("loadMessageSize", 100);
		fragmentSize = getProperty("fragmentSize", 10*1024);
		fragmentThreshold = getProperty("fragmentThreshold", 20*1024);
		fragmentDelay = getProperty("fragmentDelay", 100);
		clientQueueSize = getProperty("clientQueueSize", 1000);
		cpuBurnPerPublish = getProperty("cpuBurnPerPublish", 0);
		cpuSleepPerPublish = getProperty("cpuSleepPerPublish", 0);
		if (threads > 0) {
			asyncCommandExecuter = Executors.newFixedThreadPool(threads);
		}
		Logs.info(getPublisherName()+":"+threads + " publisher threads");
		timer = new Timer();
		TimerTask reportStats = new TimerTask() {
			@Override
			public void run() {
				String avgAdd = averageCost(addCost, addCounter);
				String avgRemove = averageCost(removeCost, removeCounter);
				String avgPublish = averageCost(publishCost, publishCounter);
				Logs.info("ADD "+avgAdd + "\t| REM " + avgRemove + "\t| PUB " + avgPublish);
			}

			private String averageCost(AtomicLong cost, AtomicLong counter) {
				long count = counter.getAndSet(0);
				if (count == 0) {
					return "avg:0us,count:0";
				} else {
					return "avg:"+(cost.getAndSet(0) / count)+"us,count:"+count;
				}
			}
		};
		timer.scheduleAtFixedRate(reportStats, 1000, 1000);
	}

	private int getProperty(String prop, int defaultVal) {
		try {
			return getIntegerProperty(prop);
		} catch (Exception e) {

		}
		return defaultVal;
	}

	@Override
	protected void publisherStarted() throws APIException {
		Logs.info(getPublisherName() + "publisherStarted");

	}

	@Override
	protected void publisherStopping() throws APIException {
		Logs.info(getPublisherName() + "publisherStopping");
		if (asyncCommandExecuter != null) {
			asyncCommandExecuter.shutdown();
		}
		timer.cancel();
	}

	@Override
	protected void publisherStopped() throws APIException {
		Logs.info(getPublisherName() + "publisherStopped");
	}

	@Override
	protected boolean isStoppable() {
		return true;
	}

	@Override
	protected void subscription(final Client client, final Topic topic,
			final boolean loaded) throws APIException {
		if (client.getMaximumQueueSize() != clientQueueSize) {
			client.setMaximumQueueSize(clientQueueSize);
		}
		TopicMessage loadMessage = topic.createLoadMessage();
		loadMessage.put(new byte[loadMessageSize]);
		prepareMessage(loadMessage);
		client.send(loadMessage);
		subscribeCounter.incrementAndGet();
	}

	private void prepareMessage(TopicMessage message)
			throws MessageException {
		if(message.size() > fragmentThreshold) {
			message.setFragmentSize(fragmentSize);
			if(fragmentDelay > 0){
				message.setFragmentedMessageLifecycle(new FragmentedMessageLifecycle(fragmentDelay));
			}
		}
	}

	@Override
	protected void unsubscription(Client client, Topic topic) {
		unsubscribeCounter.incrementAndGet();
	}

	@Override
	protected void messageFromEventPublisher(EventConnection eventConnection,
			TopicMessage message) {
		if (asyncCommandExecuter != null) {
			asyncCommandExecuter.execute(new Command(message));
		} else {
			syncExecuteCommand(message);
		}
	}

	private void syncExecuteCommand(TopicMessage message) {
		if (!this.isStarted())
			return;
		try {
			if (message.getTopicName().equals("ROOT/META")) {
				List<String> fields = message.asFields();
				if (fields.get(0).equals("ADD")) {
					long nanoTime = System.nanoTime();
					addTopic(fields.get(1), dataTopic);
					long took = System.nanoTime() - nanoTime;
					addCounter.incrementAndGet();
					addCost.addAndGet(took / 1000);
				} else if (fields.get(0).equals("REMOVE")) {
					long nanoTime = System.nanoTime();
					dataTopic.removeTopic(fields.get(1));
					long took = System.nanoTime() - nanoTime;
					removeCounter.incrementAndGet();
					removeCost.addAndGet(took / 1000);
				}
			} else {
				long nanoTime = System.nanoTime();
				prepareMessage(message);
				if(cpuBurnPerPublish > 0){
					long until = nanoTime + cpuBurnPerPublish*1000L;
					while(System.nanoTime() < until){
						// burn baby!
					}
				}
				if(cpuSleepPerPublish > 0){
					LockSupport.parkNanos(cpuSleepPerPublish*1000L);
				}
				publishMessage(message);
				long took = System.nanoTime() - nanoTime;
				publishCounter.incrementAndGet();
				publishCost.addAndGet(took / 1000);
			}
		} catch (APIException e) {
			e.printStackTrace();
		}
	}

	private class Command implements Runnable {
		private final TopicMessage message;

		public Command(TopicMessage message) {
			super();
			this.message = message;
		}

		@Override
		public void run() {
			syncExecuteCommand(message);
		}
	}
}
