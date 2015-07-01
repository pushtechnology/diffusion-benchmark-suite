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
package com.pushtechnology.benchmarks.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some JMX connection creation utility methods.
 * 
 * @author nitsanw
 * 
 */
public final class JmxHelper {

    // CHECKSTYLE:OFF
    private static final String JMX_URL_PATTERN =
            "service:jmx:rmi://%s:%d/jndi/rmi://%s:%d/%s";
    private static final String JMX_CREDENTIALS_KEY = "jmx.remote.credentials";
    private static final int DEFAULT_JNDI_PORT = 1100;
    private static final int DEFAULT_JMX_RMI_PORT = 1099;
    private static final Logger LOG = LoggerFactory.getLogger(JmxHelper.class);
    private JmxHelper() {
    }

    // CHECKSTYLE:ON

    /**
     * Connect using specific JMX ports.
     * 
     * @param host target host
     * @param service target service
     * @param user JMX user name
     * @param pass JMX password
     * @param jmxRmiPort ..
     * @param jmxJndiPort ..
     * @return a JMX connector to specified host
     * @throws IOException if connection fails/if resulting URL is malformed
     */
    public static JMXConnector getJmxConnector(final String host,
            final String service, final String user, final String pass,
            final int jmxRmiPort, final int jmxJndiPort)
            throws IOException {
        final String jmxUrl = getJmxUrl(host, service, jmxRmiPort, jmxJndiPort);
        LOG.warn("Using JMX URL: " + jmxUrl);
        final JMXServiceURL serviceUrl = new JMXServiceURL(jmxUrl);
        return getJmxConnector(user, pass, serviceUrl);
    }

    /**
     * Connect using default JMX ports.
     * 
     * @param host target host
     * @param service target service
     * @param user JMX user name
     * @param pass JMX password
     * @return a JMX connector to specified host
     * @throws IOException if connection fails/if resulting URL is malformed
     */
    public static JMXConnector getJmxConnector(final String host,
            final String service, final String user, final String pass)
            throws IOException {
        return getJmxConnector(
                host,
                service,
                user,
                pass,
                DEFAULT_JMX_RMI_PORT,
                DEFAULT_JNDI_PORT);
    }

    /**
     * Connect using JMXServiceURL.
     * 
     * @param user JMX user name
     * @param pass JMX password
     * @param serviceUrl ..
     * @return a JMX connector to specified host
     * @throws IOException if connection fails/if resulting URL is malformed
     */
    private static JMXConnector getJmxConnector(final String user,
            final String pass, final JMXServiceURL serviceUrl)
            throws IOException {
        final Map<String, String[]> env = new HashMap<String, String[]>();
        env.put(JMX_CREDENTIALS_KEY, new String[] {user, pass});

        final JMXConnector connector =
                JMXConnectorFactory.connect(serviceUrl, env);
        return connector;
    }

    /**
     * Create a JMX URL based on host and ports.
     * 
     * @param host ..
     * @param service ..
     * @param jmxRmiPort ..
     * @param jmxJndiPort ..
     * 
     * @return service:jmx:rmi://HOST:JNDI_P/jndi/rmi://HOST:RMI_P/SERVICE
     */
    private static String getJmxUrl(final String host, final String service,
            final int jmxRmiPort,
            final int jmxJndiPort) {
        return String.format(
                JMX_URL_PATTERN,
                host,
                jmxJndiPort,
                host,
                jmxRmiPort,
                service);
    }
}
