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

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.connection.ConnectionFactory;
import com.pushtechnology.diffusion.api.connection.ServerDetails;
import com.pushtechnology.diffusion.api.remote.RemoteService;

/**
 * Base service implementation.
 */
public abstract class BaseService {

    /**
     * The server details to connect to.
     */
    protected ServerDetails serverDetails = null;
    /**
     * The remote service.
     */
    protected RemoteService service = null;
    /**
     * The remote service listener.
     */
    protected BaseRemoteListener listener = null;

    /**
     * Get the server details.
     *
     * @return The server details.
     */
    @SuppressWarnings("deprecation")
    public final ServerDetails getServerDetails() {
        if (serverDetails == null) {
            try {
                serverDetails = ConnectionFactory
                    .createServerDetails("dpt://localhost:8080");
            } catch (final APIException ex) {
                Logs.warning("Failed to create server details", ex);
            }
        }
        return serverDetails;
    }

    /**
     * Get the control topic name.
     *
     * @return The name of the control topic.
     */
    public final String getControlTopicName() {
        return "RemoteControl";
    }

    /**
     * Get the name of the domain topic.
     *
     * @return The name of the domain topic.
     */
    public abstract String getDomainTopicName();

    /**
     * Get the remote service.
     *
     * @return The remote service.
     */
    public final RemoteService getRemoteService() {
        return service;
    }
}
