package clients;

public final class GentleDiffusionBenchmark extends DiffusionBenchmark {
    public static void main(final String[] args) throws Exception {
        new GentleDiffusionBenchmark().benchmark();
        System.exit(0);
    }

    long messagesDuringInterval = 0;
    long lastAvgMessagesSeen = -1;
    long avgMessagesSeen = 0;
    int avgPopSize = 0;

    @Override
    protected boolean shouldIncrementLoad(int secondsSinceIncrement) {
        if (secondsSinceIncrement > 0) {
            // get reading from monitor so that results are consistent
            messagesDuringInterval = benchmarkMonitor.getMessagesPerSecond();
            // exponential moving avg(alpha=0.1) last elements
            if (avgMessagesSeen != 0) {
                avgMessagesSeen =
                    (long) (0.1 * messagesDuringInterval + 0.9 * avgMessagesSeen);
                avgPopSize++;
            }
            else {
                avgMessagesSeen = messagesDuringInterval;
                avgPopSize = 1;
            }
        }
        // increase load as long as server is coping --> we receive more
        // messages as we add more clients
        boolean areResultsStableEnough =
            messagesDuringInterval != 0 &&
                avgMessagesSeen > lastAvgMessagesSeen &&
                avgPopSize > 10;
        if (areResultsStableEnough) {
            System.out.println("incrementing:" + avgMessagesSeen + " > " +
                lastAvgMessagesSeen);
            lastAvgMessagesSeen = avgMessagesSeen;
            avgMessagesSeen = 0;
            return true;
        }
        return false;
    }
}
