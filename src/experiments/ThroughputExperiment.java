package experiments;

import util.Factory;
import clients.ExperimentClient;

/**
 * The basic throughput experiment.
 * 
 * @author nitsanw
 *
 */
public final class ThroughputExperiment implements Runnable {

    /** the experiment loop. */
    private final ExperimentControlLoop loop;

    /**
     * @param settings ...
     */
    public ThroughputExperiment(CommonExperimentSettings settings) {
        loop = new ExperimentControlLoop(settings);
        Factory<ExperimentClient> clientFactory =
                new DefaultClientFactory(loop.getClientSettings(), 
                        loop.getExperimentCounters());
        loop.setClientFactory(clientFactory);
        ExperimentLoadStrategy defaultLoadStrategy =
                new DefaultLoadStrategy(loop.getClientSettings(), 
                        loop.getExperimentCounters());
        loop.setLoadStartegy(defaultLoadStrategy);
    }

    @Override
    public void run() {
        loop.run();
    }

}
