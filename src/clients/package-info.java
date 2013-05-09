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

/**
 * Client behaviour implementations. Experiments will usually involve a host
 * of clients hooking up to a server and interacting with it(send/ subscribe/ 
 * un-subscribe) over a period of time. Clients report their observations via
 * the ExperimentCounters object and the MessageCountingClient implements most
 * of the required reporting for clients.`
 * 
 * @author nitsanw
 */
package clients;

