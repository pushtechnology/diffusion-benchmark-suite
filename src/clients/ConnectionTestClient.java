package clients;

import com.pushtechnology.diffusion.api.APIException;
import com.pushtechnology.diffusion.api.Credentials;
import com.pushtechnology.diffusion.api.ServerConnection;
import com.pushtechnology.diffusion.api.ServerConnectionListener;
import com.pushtechnology.diffusion.api.client.ExternalClientConnection;
import com.pushtechnology.diffusion.api.message.MessageException;
import com.pushtechnology.diffusion.api.message.TopicMessage;
import com.pushtechnology.diffusion.api.topic.TopicStatus;

import java.util.concurrent.atomic.AtomicLong;

import publishers.InjectionPublisher;
import util.IntervalCounterMonitor;


public final class ConnectionTestClient
    implements ServerConnectionListener
{
    public static final String CONNECT_STRING;

    static
    {
        CONNECT_STRING = System.getProperty("connect.string", "ws://localhost:8080");
    }

    private static String latestMessage;
    private static final AtomicLong messageCounter = new AtomicLong();

    public static void main(final String[] args)
        throws Exception
    {
        final IntervalCounterMonitor intervalCounterMonitor = new IntervalCounterMonitor(messageCounter, "sessions cycled");
        ConnectionTestClient testClient = new ConnectionTestClient();

        Thread monitorThread = new Thread(intervalCounterMonitor);
        monitorThread.setDaemon(true);
        monitorThread.setName("counter-monitor-thread");
        monitorThread.start();

        ConnectionRunner connectionRunner = new ConnectionRunner(testClient);
        Thread connectionThread = new Thread(connectionRunner);
        connectionThread.setDaemon(true);
        connectionThread.setName("connection-runner-thread");
        connectionThread.start();

        System.out.println("Hit any key to exit");
        System.in.read();

        connectionRunner.halt();
        connectionThread.interrupt();

        intervalCounterMonitor.halt();
        monitorThread.interrupt();

        System.out.println("exiting: latest message = " + latestMessage);
    }

    @Override
    public void serverConnected(final ServerConnection serverConnection)
    {
    }

    @Override
    public void messageFromServer(final ServerConnection serverConnection, final TopicMessage topicMessage)
    {
        try
        {
            latestMessage = topicMessage.asString();
            messageCounter.lazySet(messageCounter.get() + 1);
        }
        catch (MessageException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void serverTopicStatusChanged(final ServerConnection serverConnection, final String s, final TopicStatus topicStatus)
    {
    }

    @Override
    public void serverRejectedCredentials(final ServerConnection serverConnection, final Credentials credentials)
    {
    }

    @Override
    public void serverDisconnected(final ServerConnection serverConnection)
    {
    }

    public static final class ConnectionRunner implements Runnable
    {
        private final ServerConnectionListener listener;
        private volatile boolean running;

        public ConnectionRunner(final ServerConnectionListener listener)
        {
            this.listener = listener;
        }

        public void halt()
        {
            running = false;
        }

        @Override
        public void run()
        {
            running = true;

            while (running)
            {
                try
                {
                    long currentCounter = messageCounter.get();
                    ExternalClientConnection connection = new ExternalClientConnection(listener, CONNECT_STRING);
                    connection.connect();
                    connection.subscribe(InjectionPublisher.INJECTOR_ROOT);

                    while (currentCounter == messageCounter.get())
                    {
                        // busy spin
                    }

                    connection.close();
                }
                catch (APIException e)
                {
                    e.printStackTrace();
                }

            }
        }
    }
}
