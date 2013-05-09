package util;

import java.util.Arrays;

/**
 * A rolling average construct for power of 2 sized windows.
 * @author nitsanw
 * 
 */
public final class RollingAverage {
    /** data set. */
    private final long[] dataPoints;
    /** index mask for % replacement. */
    private final int mask;
    /** current index. */
    private int index = 0;
    /** sum. */
    private long sum = 0;

    /**
     * @param size must be power of 2.
     */
    public RollingAverage(int size) {
        if (Integer.bitCount(size) != 1) {
            throw new IllegalArgumentException("only works for power of 2 "
                    + "size");
        }
        dataPoints = new long[size];
        mask = size - 1;
    }

    /**
     * resets all data.
     */
    public void reset() {
        Arrays.fill(dataPoints, 0L);
        index = 0;
        sum = 0;
    }

    /**
     * @param newDataPoint a new data point
     */
    public void sample(long newDataPoint) {
        int indexLocal = index & mask;
        long old = dataPoints[indexLocal];
        dataPoints[indexLocal] = newDataPoint;
        sum = sum - old + newDataPoint;
        index++;
    }

    /**
     * @return rolling average
     */
    public long avg() {
        int size = size();
        if (size == 0) {
            return 0;
        }
        return sum / size;
    }

    /**
     * @return number of samples in average
     */
    public int size() {
        return Math.min(dataPoints.length, index);
    }
}
