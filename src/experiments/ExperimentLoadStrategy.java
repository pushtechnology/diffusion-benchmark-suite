package experiments;

/**
 * @author nitsanw
 *
 */
public interface ExperimentLoadStrategy {
    /**
     * @param lastIncrementTime
     * @return
     */
    boolean shouldIncrementLoad(long lastIncrementTime);
    
    /**
     * @param testStartTime
     * @return
     */
    boolean testNotOver(long testStartTime);
}
