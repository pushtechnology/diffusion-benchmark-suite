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
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.util.concurrent.atomic.AtomicLong;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import com.pushtechnology.diffusion.api.ServerConnectionListener;
import com.pushtechnology.diffusion.api.client.ExternalClientConnection;
import com.pushtechnology.diffusion.api.connection.ConnectionFactory;
import com.pushtechnology.diffusion.api.connection.ServerDetails;
import com.pushtechnology.diffusion.utils.JVMSupport;

class ClientConnectionFactory {
    private final static boolean LOG_EXCEPTION = Boolean.getBoolean("log.exception");
    private final AtomicLong clientsCreatedCounter = new AtomicLong(0L);
    private final AtomicLong connectionAttemptsCounter;
    private final AtomicLong connectionRefusedCounter;
    private final String[] connectStrings;
    private final String[] networkInterfaces;
    private SSLContext sslContext; 
    public ClientConnectionFactory(AtomicLong connectionAttemptsCounter,
    AtomicLong connectionRefusedCounter,
    String[] connectStrings,
    String[] networkInterfaces) {
        super();
        this.connectionRefusedCounter = connectionRefusedCounter;
        this.connectStrings = connectStrings;
        this.networkInterfaces = networkInterfaces;
        this.connectionAttemptsCounter = connectionAttemptsCounter;
    }

    ExternalClientConnection createConnection(ServerConnectionListener listener,String topic) {

        // choose binding address via local.interface
        try {
            connectionAttemptsCounter.incrementAndGet();
            int rotator = (int)clientsCreatedCounter.incrementAndGet();

            String url = connectStrings[rotator%
                connectStrings.length];
            ServerDetails serverDetails;
                serverDetails = createServerDetails(url);
            ExternalClientConnection connection =
                new ExternalClientConnection(
                    listener,serverDetails);
            rotateNic(connection,rotator);
            connection.connect(topic);
            return connection;
        }
        catch (Exception e) {
            if(LOG_EXCEPTION){
                e.printStackTrace();
            }
            connectionRefusedCounter.incrementAndGet();
            return null;
        }
    }
    /**
     * Create the Diffusion ServerDetails, setting the input buffer size and SSL context if the connection is secure
     * 
     * @param url
     *            Diffusion server URL
     * @return {@link ServerDetails}
     * @throws Throwable
     */
    private ServerDetails createServerDetails(final String url) throws Exception
    {
        final ServerDetails serverDetails = ConnectionFactory.createServerDetails(url);
        if (serverDetails.isSecureConnection())
        {
            serverDetails.setSSLContext(createSSLContextWhichTrustsDiffusion());
            serverDetails.setInputBufferSize(64 * 1024); // 64KB, to match the default in Connectors.xml
        }
        return serverDetails;
    }
    /**
     * In order to allow tests to connect to Diffusion over SSL based protocols, the client needs to trust the server's
     * certificate.
     * 
     * The client can be configured to use a specific SSLContext, this method creates an SSL certificate which is
     * configured to trust the Diffusion certificate.
     * 
     * To do this, it finds Diffusion's keystore, extracts the root of the certificate chain, and puts it in a new
     * keystore, which it then adds to a trust manager, which it uses to build an SSLContext.
     * 
     * @return A new SSLContext which is configured to trust the Diffusion's certificate
     * @throws KeyStoreException
     * @throws RuntimeException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws KeyManagementException
     */
    private SSLContext createSSLContextWhichTrustsDiffusion() throws KeyStoreException,
            RuntimeException,
            NoSuchAlgorithmException,
            CertificateException,
            IOException,
            NoSuchProviderException,
            KeyManagementException
    {

        if(sslContext!=null){
            return sslContext;
        }
        final KeyStore diffusionKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
        final FileInputStream input = new FileInputStream(new File(new File(System.getenv("DIFFUSION_HOME"),
                "etc"), "keystore"));
        try
        {
            diffusionKeystore.load(input, null);
        }
        finally
        {
            input.close();
        }
        assert diffusionKeystore.containsAlias("diffusion") : "Unable to load certificate";
        final Certificate[] chain = diffusionKeystore.getCertificateChain("diffusion");

        final KeyStore clientKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
        clientKeystore.load(null);
        // Presumably we only need to trust the last element in the certificate chain.
        clientKeystore.setCertificateEntry("diffusion-root", chain[chain.length - 1]);
        final TrustManagerFactory trustManagerFactory = JVMSupport.getTrustManagerFactory();
        trustManagerFactory.init(clientKeystore);
        sslContext = SSLContext.getInstance("SSL");
        sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
        return sslContext;
    }

    void rotateNic(ExternalClientConnection connection,int rotator) {
        if (networkInterfaces!=null&&networkInterfaces.length>0) {
            String nic =
                networkInterfaces[rotator%
                    networkInterfaces.length];
            ServerDetails serverDetails =
                connection.getConnectionDetails().getServerDetails().get(0);
            if (serverDetails!=null && !nic.isEmpty()) {
                InetSocketAddress paramSocketAddress = new InetSocketAddress(nic,0);
                if(paramSocketAddress.isUnresolved()){
                    throw new IllegalArgumentException(nic + " could not be resolved");
                }
                serverDetails
                    .setLocalSocketAddress(paramSocketAddress);
            }
        }
    }
}
