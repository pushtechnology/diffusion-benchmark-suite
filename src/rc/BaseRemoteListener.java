package rc;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.ClientDetails;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.remote.RemoteRequest;
import com.pushtechnology.diffusion.api.remote.RemoteServiceCloseReason;
import com.pushtechnology.diffusion.api.remote.RemoteServiceListener;
import com.pushtechnology.diffusion.api.topic.TopicSelector;

public class BaseRemoteListener implements RemoteServiceListener {

	private CountDownLatch registrationLatch = null;
	protected final BaseService service;
	
	public BaseRemoteListener(BaseService service) {
		this.service = service;
	}
	
	@Override
	public void clientConnected(ClientDetails clientDetails) {
		Logs.fine("clientConnected(" + clientDetails.getClientID() + ")");
	}

	@Override
	public void clientDetailsChanged(ClientDetails clientDetails) {
		Logs.fine("clientDetailsChanged(" + clientDetails.getClientID() + ")");
	}

	@Override
	public void clientDisconnected(String clientId) {
		Logs.fine("clientDisconnected(" + clientId + ")");
	}

	@Override
	public void clientFetch(ClientDetails clientDetails, String topicName, List<String> headers) {
		Logs.fine("clientFetch(" + clientDetails.getClientID() + ", " + topicName + ")");
		try {
			service.getRemoteService().sendFetchReply(clientDetails.getClientID(), topicName, headers);
		}
		catch(APIException ex) {
			Logs.warning("Failed to send fetch reply to client " + clientDetails.getClientID() + " for topic " + topicName);
		}
	}

	@Override
	public void clientSubscribe(ClientDetails clientDetails, String topicName) {
		Logs.fine("clientSubscribe(" + clientDetails.getClientID() + ", " + topicName + ")");
		try {
			service.getRemoteService().subscribeClient(clientDetails.getClientID(), topicName);
		}
		catch(APIException ex) {
			Logs.warning("Failed to subscribe client " + clientDetails.getClientID() + " to topic " + topicName, ex);
		}
	}

	@Override
	public void clientSubscribe(ClientDetails clientDetails, TopicSelector topicSelector) {
		Logs.fine("clientSubscribe(" + clientDetails.getClientID() + ", " + topicSelector + ")");
	}

	@Override
	public void clientUnsubscribe(String clientId, String topicName, boolean hasSubscribers) {
		Logs.fine("clientUnSubscribe(" + clientId + ", " + topicName);
	}

	@Override
	public void closed(RemoteServiceCloseReason reason) {
		Logs.fine("closed(" + reason + ")");
	}

	@Override
	public void messageFromClient(ClientDetails clientDetails, String topicName,
			TopicMessage message) {
		Logs.fine("messageFromClient(" + clientDetails.getClientID() + ", " + topicName + ")");
	}

	@Override
	public void messageFromPublisher(TopicMessage message) {
		Logs.fine("messageFromPublisher(" + message.getTopicName() + ")");
	}

	@Override
	public void registerFailed(String errorMessage) {
		if(registrationLatch != null) {
			registrationLatch.countDown();
		}
		Logs.warning("Registration failed: " + errorMessage);
	}

	@Override
	public void registered() {
		if(registrationLatch != null) {
			registrationLatch.countDown();
		}
		Logs.fine("Registered");
	}

	@Override
	public void serviceRequest(RemoteRequest request) {
		Logs.fine("serviceRequest(" + request + ")");

	}

	@Override
	public void topicAddFailed(String topicName, String errorMessage) {
		Logs.warning("topicAddFailed(" + topicName + ") : " + errorMessage);
	}

	@Override
	public void topicSubscribeFailed(String clientId, String topicName, String errorMessage) {
		Logs.warning("topicSubscribeFailed(" + clientId + ", " + topicName + ") : " + errorMessage);
	}

	/**
	 * Set up a latch for synchronous registration notifications.
	 */
	public void resetRegisterLatch() {
		registrationLatch = new CountDownLatch(1);
	}
	
	/**
	 * Synchronous wait for registration notifications.
	 * 
	 * @param timeout
	 * @param unit
	 */
	public void waitForRegistration(long timeout, TimeUnit unit) {
		if(registrationLatch == null) {
			throw new IllegalStateException("Registration Latch not initialised");
		}
		
		try {
			registrationLatch.await(timeout, unit);
		}
		catch(InterruptedException ignore) {
		}
	}
}
