package experiments;

public class DefaultLoadStrategy implements ExperimentLoadStrategy {
    private final CommonClientSettings clientSettings;


    public DefaultLoadStrategy(CommonClientSettings clientSettings) {
        this.clientSettings = clientSettings;
    }
    @Override
    public boolean testNotOver(long testStartTime) {
        long timeSinceStart =
                System.currentTimeMillis() - testStartTime;
        long maxTestTimeMillis =
                clientSettings.getMaxTestTimeMillis();
        return timeSinceStart < maxTestTimeMillis;

    }

    @Override
    public boolean shouldIncrementLoad(long lastIncrementTime) {
        long timeSinceIncrement = System.currentTimeMillis()
                - lastIncrementTime;
        return timeSinceIncrement >= clientSettings
                .getClientIncrementPauseSeconds() * 1000;
    }

}