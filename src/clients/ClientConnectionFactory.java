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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.cert.Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import monitoring.ExperimentCounters;
import util.Factory;

import com.pushtechnology.diffusion.api.ServerConnectionListener;
import com.pushtechnology.diffusion.api.client.ExternalClientConnection;
import com.pushtechnology.diffusion.api.connection.ConnectionFactory;
import com.pushtechnology.diffusion.api.connection.ServerDetails;
import com.pushtechnology.diffusion.utils.JVMSupport;

import experiments.CommonExperimentSettings;

/**
 * This is a factory for ExternalClientConnections given a set of diffusion urls
 * to connect to and a set of local interfaces to bind to. Urls must be
 * available on the respective local interfaces.
 * 
 * @author nwakart
 * 
 */
public class ClientConnectionFactory implements
        Factory<ExternalClientConnection> {
    // CHECKSTYLE:OFF
    private static final int SSL_BUFFER_SIZE = 64 * 1024;
    private static final boolean VERBOSE = Boolean.getBoolean("verbose");
    private final ExperimentCounters experimentCounters;
    private final CommonExperimentSettings clientSettings;
    private SSLContext cachedSslContext;

    private Factory<ExperimentClient> clientFactory;

    // CHECKSTYLE:ON
    /**
     * @param experimentCountersP ...
     * @param clientSettingsP ...
     * @param clientFactoryP ...
     */
    public ClientConnectionFactory(ExperimentCounters experimentCountersP,
            CommonExperimentSettings clientSettingsP,
            Factory<ExperimentClient> clientFactoryP) {
        this.experimentCounters = experimentCountersP;
        this.clientSettings = clientSettingsP;
        this.clientFactory = clientFactoryP;
    }

    /**
     * @param client ...
     * @param topics ...
     * @return a new connection or null if failed to create one.
     */
    public final ExternalClientConnection createConnection(
            ServerConnectionListener client, String... topics) {

        // choose binding address via local.interface
        try {
            experimentCounters.incConnectionAttemptsCounter();
            int rotator = (int)
                    experimentCounters.getConnectionAttemptsCounter();

            String[] diffusionUrls = clientSettings.getDiffusionUrls();
            String url = diffusionUrls[rotator % diffusionUrls.length];
            ServerDetails serverDetails = createServerDetails(url);
            bindToLocalInterface(serverDetails, rotator);
            ExternalClientConnection connection =
                    new ExternalClientConnection(client, serverDetails);
            if (topics != null) {
                connection.connect(topics);
            } else {
                connection.connect();
            }
            return connection;
        } catch (Exception e) {
            if (VERBOSE) {
                e.printStackTrace();
            }
            experimentCounters.incConnectionRefusedCounter();
            return null;
        }
    }

    /**
     * Create the Diffusion ServerDetails, setting the input buffer size and SSL
     * context if the connection is secure.
     * 
     * @param url Diffusion server URL
     * @return {@link ServerDetails}
     * @throws Exception when bad stuff happens
     */
    private ServerDetails createServerDetails(final String url)
            throws Exception {
        final ServerDetails serverDetails = ConnectionFactory
                .createServerDetails(url);
        if (serverDetails.isSecureConnection()) {
            serverDetails.setSSLContext(createSSLContextWhichTrustsDiffusion());
            // set buffer size to match Connectors.xml default
            serverDetails.setInputBufferSize(SSL_BUFFER_SIZE);
        }
        return serverDetails;
    }

    /**
     * In order to allow tests to connect to Diffusion over SSL based protocols,
     * the client needs to trust the server's certificate.
     * 
     * The client can be configured to use a specific SSLContext, this method
     * creates an SSL certificate which is configured to trust the Diffusion
     * certificate.
     * 
     * To do this, it finds Diffusion's keystore, extracts the root of the
     * certificate chain, and puts it in a new keystore, which it then adds to a
     * trust manager, which it uses to build an SSLContext.
     * 
     * @return A new SSLContext which is configured to trust the Diffusion's
     *         certificate
     * @throws Exception when bad stuff happens
     */
    private SSLContext createSSLContextWhichTrustsDiffusion()
            throws Exception {

        // caching sslContext for reuse
        if (cachedSslContext != null) {
            return cachedSslContext;
        }
        final KeyStore diffusionKeystore = KeyStore.getInstance(KeyStore
                .getDefaultType());
        final FileInputStream input = new FileInputStream(new File(new File(
                System.getenv("DIFFUSION_HOME"), "etc"), "keystore"));
        try {
            diffusionKeystore.load(input, null);
        } finally {
            input.close();
        }
        // assert able to load certificate
        assert diffusionKeystore.containsAlias("diffusion");
        final Certificate[] chain = diffusionKeystore
                .getCertificateChain("diffusion");

        final KeyStore clientKeystore = KeyStore.getInstance(KeyStore
                .getDefaultType());
        clientKeystore.load(null);
        // Presumably we only need to trust the last element in the certificate
        // chain.
        clientKeystore.setCertificateEntry("diffusion-root",
                chain[chain.length - 1]);
        final TrustManagerFactory trustManagerFactory = JVMSupport
                .getTrustManagerFactory();
        trustManagerFactory.init(clientKeystore);
        cachedSslContext = SSLContext.getInstance("SSL");
        cachedSslContext.init(null, trustManagerFactory.getTrustManagers(),
                null);
        return cachedSslContext;
    }

    /**
     * @param serverDetails ...
     * @param rotator ...
     */
    private void bindToLocalInterface(ServerDetails serverDetails,
            int rotator) {
        String[] localInterfaces = clientSettings.getLocalInterfaces();
        if (localInterfaces != null && localInterfaces.length > 0) {
            String nic = localInterfaces[rotator % localInterfaces.length];
            if (serverDetails != null && !nic.isEmpty()) {
                InetSocketAddress paramSocketAddress = new InetSocketAddress(
                        nic, 0);
                if (paramSocketAddress.isUnresolved()) {
                    throw new IllegalArgumentException(nic
                            + " could not be resolved");
                }
                serverDetails.setLocalSocketAddress(paramSocketAddress);
            }
        }
    }

    @Override
    public final ExternalClientConnection create() {
        long numberCurrentlyConnected =
                experimentCounters.getCurrentlyConnected();
        if (numberCurrentlyConnected < clientSettings.getMaxClients()) {
            ExperimentClient client = clientFactory.create();
            return createConnection(client, client.getInitialTopics());
        } else {
            return null;
        }
    }

    @Override
    public final void close() throws IOException {
        clientFactory.close();
    }
}
