package util;

import java.util.concurrent.atomic.AtomicLong;

public class IntervalCounterMonitor implements Runnable
{
    private final AtomicLong counter;
    private final String message;
    private volatile boolean running = true;

    public IntervalCounterMonitor(final AtomicLong counter, final String message)
    {
        this.counter = counter;
        this.message = message;
    }

    public void halt()
    {
        running = false;
    }

    @Override
    public void run()
    {
        long lastTimeStampNanos = System.nanoTime();
        long lastMessageCount = 0;

        while (running)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                continue;
            }

            long newMessageCount = counter.get();
            long newTimeStamp = System.nanoTime();

            long numMessages = newMessageCount - lastMessageCount;
            long intervalNanos = newTimeStamp - lastTimeStampNanos;

            System.out.format("%,d %s in %,dns\n", numMessages, message, intervalNanos);

            lastMessageCount = newMessageCount;
            lastTimeStampNanos = newTimeStamp;
        }
    }
}