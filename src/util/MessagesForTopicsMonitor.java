package util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class MessagesForTopicsMonitor implements Runnable
{
    private final AtomicLong messageCounter;
    private volatile boolean running = true;
	private final AtomicLong topicsCounter;

    public MessagesForTopicsMonitor(final AtomicLong messageCounter, final AtomicLong topicsCounter)
    {
        this.messageCounter = messageCounter;
        this.topicsCounter = topicsCounter;
    }

    public void halt()
    {
        running = false;
    }

    @Override
    public void run()
    {
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS");

        while (running)
        {
        	long start = messageCounter.get();
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                continue;
            }

            System.out.format("%s - %,d messages for %d topics\n", format.format(new Date()), messageCounter.get() - start, topicsCounter.get());
        }
    }
}