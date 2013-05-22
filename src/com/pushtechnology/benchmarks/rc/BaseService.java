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
package com.pushtechnology.benchmarks.rc;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.Logs;
import com.pushtechnology.diffusion.api.connection.ConnectionFactory;
import com.pushtechnology.diffusion.api.connection.ServerDetails;
import com.pushtechnology.diffusion.api.remote.RemoteService;

public abstract class BaseService {

	protected ServerDetails serverDetails = null;
	protected RemoteService service = null;
	protected BaseRemoteListener listener = null;
	
	public ServerDetails getServerDetails() {
		if(serverDetails == null) {
			try {
				serverDetails = ConnectionFactory.createServerDetails("dpt://localhost:8080");
			}
			catch(APIException ex) {
				Logs.warning("Failed to create server details", ex);
			}
		}
		return serverDetails;
	}
	
	public String getControlTopicName() {
		return "RemoteControl";
	}
	
	public abstract String getDomainTopicName();
	
	public RemoteService getRemoteService() {
		return service;
	}
}
