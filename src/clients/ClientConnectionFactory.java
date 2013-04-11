package clients;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicLong;

import com.pushtechnology.diffusion.api.ServerConnectionListener;
import com.pushtechnology.diffusion.api.client.ExternalClientConnection;
import com.pushtechnology.diffusion.api.connection.ServerDetails;

class ClientConnectionFactory {
    private final static boolean LOG_EXCEPTION = Boolean.getBoolean("log.exception");
    private final AtomicLong clientsCreatedCounter = new AtomicLong(0L);
    private final AtomicLong connectionAttemptsCounter;
    private final AtomicLong connectionRefusedCounter;
    private final String[] connectStrings;
    private final String[] networkInterfaces;

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
            ExternalClientConnection connection =
                new ExternalClientConnection(
                    listener,url);
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