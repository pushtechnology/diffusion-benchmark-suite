package util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class AccumulatedCounterMonitor implements Runnable
{
    private final AtomicLong counter;
    private final String message;
    private volatile boolean running = true;

    public AccumulatedCounterMonitor(final AtomicLong counter, final String message)
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
        SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss z");

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

            System.out.format("%,d %s at %s\n", counter.get(), message, format.format(new Date()));
        }
    }
}