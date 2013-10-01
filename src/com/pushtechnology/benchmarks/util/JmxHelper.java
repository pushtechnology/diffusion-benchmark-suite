package com.pushtechnology.benchmarks.util;

import java.io.IOException;
import java.util.HashMap;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.pushtechnology.diffusion.api.Logs;

/**
 * Some JMX connection creation utility methods.
 * 
 * @author nitsanw
 * 
 */
public final class JmxHelper {

    // CHECKSTYLE:OFF
    private static final int DEFAULT_JNDI_PORT = 1100;
    private static final int DEFAULT_JMX_RMI_PORT = 1099;

    private JmxHelper() {
    }

    // CHECKSTYLE:ON

    /**
     * @param host target host
     * @param service target service
     * @param user JMX user name
     * @param pass JMX password
     * @param jmxRmiPort ..
     * @param jmxJndiPort ..
     * @return a JMX connector to specified host
     * @throws IOException if connection fails/if resulting URL is malformed
     */
    public static JMXConnector getJmxConnector(String host, String service,
            String user, String pass, int jmxRmiPort, int jmxJndiPort)
            throws IOException {
        String jmxUrl = getJmxUrl(host, service, jmxRmiPort, jmxJndiPort);
        JMXServiceURL serviceUrl = new JMXServiceURL(jmxUrl);
        return getJMXConnector(user, pass, serviceUrl);
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
    public static JMXConnector getJmxConnector(String host, String service,
            String user, String pass)
            throws IOException {
        String jmxUrl = getJmxUrl(host, service);
        Logs.advice("Using JMX URL: " + jmxUrl);
        JMXServiceURL serviceUrl = new JMXServiceURL(jmxUrl);
        return getJMXConnector(user, pass, serviceUrl);
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
    private static JMXConnector getJMXConnector(String user, String pass,
            JMXServiceURL serviceUrl) throws IOException {
        HashMap<String, String[]> env = new HashMap<String, String[]>();
        env.put("jmx.remote.credentials", new String[] {user, pass});

        JMXConnector connector = JMXConnectorFactory.connect(serviceUrl, env);
        return connector;
    }

    /**
     * Create a JMX URL based on host using default JMX ports.
     * 
     * @param host ..
     * @param service ..
     * @return see {@link #getJmxUrl(String, int, int)}
     */
    public static String getJmxUrl(String host, String service) {
        return getJmxUrl(host, service,
                DEFAULT_JMX_RMI_PORT, DEFAULT_JNDI_PORT);
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
    public static String getJmxUrl(String host, String service, int jmxRmiPort,
            int jmxJndiPort) {
        return String.format(
                "service:jmx:rmi://%s:%d/jndi/rmi://%s:%d/%s",
                host,
                jmxJndiPort,
                host,
                jmxRmiPort,
                service);
    }
}
