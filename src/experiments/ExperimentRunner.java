package experiments;

/**
 * A main class for all experiments.
 * 
 * @author nitsanw
 * 
 */
public final class ExperimentRunner {
    /**
     * Never create me...
     */
    private ExperimentRunner() {

    }

    /**
     * @param args expect experiment class name
     * @throws Exception 
     */
    public static void main(final String[] args) throws Exception {
        BaseExperiment experiment = ((BaseExperiment) Class.forName(args[0]).
                newInstance());
        experiment.run();
    }

}
