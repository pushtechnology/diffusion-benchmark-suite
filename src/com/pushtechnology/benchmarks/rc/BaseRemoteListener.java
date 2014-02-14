/*
 * Copyright 2013, 2014 Push Technology
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
package com.pushtechnology.benchmarks.rc;

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

/**
 * Base implementation of a RemoteServiceListener.
 */
public class BaseRemoteListener implements RemoteServiceListener {

    /**
     * Registration latch.
     */
    private CountDownLatch registrationLatch = null;
    /**
     * The service the listener is for.
     */
    protected final BaseService service;

    /**
     * Constructor.
     * @param service The service the listener is for.
     */
    public BaseRemoteListener(final BaseService service) {
        this.service = service;
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void clientConnected(ClientDetails clientDetails) {
        Logs.fine("clientConnected(" + clientDetails.getClientID() + ")");
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void clientDetailsChanged(ClientDetails clientDetails) {
        Logs.fine("clientDetailsChanged(" + clientDetails.getClientID() + ")");
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void clientDisconnected(String clientId) {
        Logs.fine("clientDisconnected(" + clientId + ")");
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void clientFetch(ClientDetails clientDetails,
            String topicName, List<String> headers) {
        Logs.fine("clientFetch(" + clientDetails.getClientID() + ", "
                + topicName + ")");
        try {
            service.getRemoteService().sendFetchReply(
                    clientDetails.getClientID(), topicName, headers);
        } catch (APIException ex) {
            Logs.warning("Failed to send fetch reply to client "
                    + clientDetails.getClientID() + " for topic " + topicName);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void clientSubscribe(ClientDetails clientDetails,
            String topicName) {
        Logs.fine("clientSubscribe(" + clientDetails.getClientID() + ", "
                + topicName + ")");
        try {
            service.getRemoteService().subscribeClient(
                    clientDetails.getClientID(), topicName);
        } catch (APIException ex) {
            Logs.warning(
                    "Failed to subscribe client " + clientDetails.getClientID()
                            + " to topic " + topicName, ex);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void clientSubscribe(ClientDetails clientDetails,
            TopicSelector topicSelector) {
        Logs.fine("clientSubscribe(" + clientDetails.getClientID() + ", "
                + topicSelector + ")");
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void clientUnsubscribe(String clientId, String topicName,
            boolean hasSubscribers) {
        Logs.fine("clientUnSubscribe(" + clientId + ", " + topicName);
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void closed(RemoteServiceCloseReason reason) {
        Logs.fine("closed(" + reason + ")");
    }

    @SuppressWarnings("deprecation")
    @Override
    public void messageFromClient(ClientDetails clientDetails,
            String topicName,
            TopicMessage message) {
        Logs.fine("messageFromClient(" + clientDetails.getClientID() + ", "
                + topicName + ")");
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void messageFromPublisher(TopicMessage message) {
        Logs.fine("messageFromPublisher(" + message.getTopicName() + ")");
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void registerFailed(String errorMessage) {
        if (registrationLatch != null) {
            registrationLatch.countDown();
        }
        Logs.warning("Registration failed: " + errorMessage);
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void registered() {
        if (registrationLatch != null) {
            registrationLatch.countDown();
        }
        Logs.fine("Registered");
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void serviceRequest(RemoteRequest request) {
        Logs.fine("serviceRequest(" + request + ")");

    }

    @SuppressWarnings("deprecation")
    @Override
    public final void topicAddFailed(String topicName, String errorMessage) {
        Logs.warning("topicAddFailed(" + topicName + ") : " + errorMessage);
    }

    @SuppressWarnings("deprecation")
    @Override
    public final void topicSubscribeFailed(String clientId, String topicName,
            String errorMessage) {
        Logs.warning("topicSubscribeFailed(" + clientId + ", " + topicName
                + ") : " + errorMessage);
    }

    /**
     * Set up a latch for synchronous registration notifications.
     */
    public final void resetRegisterLatch() {
        registrationLatch = new CountDownLatch(1);
    }

    /**
     * Synchronous wait for registration notifications.
     * 
     * @param timeout The timeout period.
     * @param unit The timeout unit.
     */
    public final void waitForRegistration(final long timeout,
            final TimeUnit unit) {
        if (registrationLatch == null) {
            throw new IllegalStateException(
                    "Registration Latch not initialised");
        }

        try {
            registrationLatch.await(timeout, unit);
        } catch (final InterruptedException ignore) {
        }
    }
}
