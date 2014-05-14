package com.pushtechnology.benchmarks.control.clients;

import java.util.concurrent.CountDownLatch;

import com.pushtechnology.diffusion.client.Diffusion;
import com.pushtechnology.diffusion.client.session.Session;

/**
 * Base implementation of a control client.
 * <P>
 * Makes initialising the client behavior blocking.
 *
 * @author matt - created 14 May 2014
 */
public abstract class BaseControlClient {
    private final Session session;
    private final CountDownLatch initialised;

    /**
     * Constructor.
     * @param countDown Number of times initialised must be called before start unblocks.
     */
    public BaseControlClient(int countDown) {
        session = Diffusion.sessions().open("dpt://localhost:8080");
        initialised = new CountDownLatch(countDown);
    }

    /**
     * Start the session. Invokes the initialise method. Blocks until initialised is called the required number
     * of times.
     *
     * @throws InterruptedException
     */
    public final void start() throws InterruptedException {
        session.start();
        initialise(session);
        initialised.await();
    }

    public final void stop() {
        session.close();
    }

    public final void initialised() {
        initialised.countDown();
    }

    public abstract void initialise(Session session);
}
