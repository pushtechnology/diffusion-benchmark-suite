package com.pushtechnology.benchmarks.monitoring;

import java.io.PrintStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.HdrHistogram.AbstractHistogram;
import org.HdrHistogram.ConcurrentHistogram;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.WriterReaderPhaser;

/**
 * This is where all the latency heavy lifting is done.
 * 
 * @author abagehot
 *
 */
public class LatencyMonitor {

	private final WriterReaderPhaser recordingPhaser = new WriterReaderPhaser();

	private PeriodicLatencyHistogram inactiveHistogram;
	private volatile PeriodicLatencyHistogram activeHistogram;

	private boolean isWarmup=true;

    /**
     * Histogram significant digits
     */
    private static final int SIG_DIGITS = 3;
    /**
     * The ratio used by the histogram to scale its output.
     * We measure time with System.nanoTime() - therefore with a
     * scaling of 1000 the reported time is in micros...
     */
    private static final double HISTOGRAM_SCALING_RATIO = 1000.0;
	
	public LatencyMonitor() {
		inactiveHistogram = new PeriodicLatencyHistogram(true);
		activeHistogram = new PeriodicLatencyHistogram(true);
	}

	public void recordLatencyValue(long elapsed) {
        
		if(isWarmup)
			return;
		
		long criticalValueAtEnter = recordingPhaser
				.writerCriticalSectionEnter();
		try {
			activeHistogram.recordValue(elapsed);
		} finally {
			recordingPhaser.writerCriticalSectionExit(criticalValueAtEnter);
		}
	}

	public synchronized PeriodicLatencyHistogram getIntervalHistogram() {
		
		PeriodicLatencyHistogram empty = new PeriodicLatencyHistogram(this.isWarmup);
		try {
			recordingPhaser.readerLock();
			inactiveHistogram = empty;
			performIntervalSample();
			return inactiveHistogram;
		} finally {
			recordingPhaser.readerUnlock();
		}
	}

	private void performIntervalSample() {
		// inactiveHistogram.reset();
		try {
			recordingPhaser.readerLock();

			// Swap active and inactive histograms:
			final PeriodicLatencyHistogram tempHistogram = inactiveHistogram;
			inactiveHistogram = activeHistogram;
			activeHistogram = tempHistogram;

			// Mark end time of previous interval and start time of new one:
			long now = System.currentTimeMillis();
			activeHistogram.setStartTimeStamp(now);
			inactiveHistogram.setEndTimeStamp(now);

			// Make sure we are not in the middle of recording a value on the
			// previously active histogram:

			// Flip phase to make sure no recordings that were in flight
			// pre-flip are still active:
			recordingPhaser
					.flipPhase(500000L /* yield in 0.5 msec units if needed */);
		} finally {
			recordingPhaser.readerUnlock();
		}
	}

	/**
	 * This class encapsulates a latency histogram for a period of time.
	 * 
	 * It can be tagged for example as measurements taken during warmup, or steady
	 * state.
	 * 
	 * @author abagehot
	 *
	 */
	public static class PeriodicLatencyHistogram {

		private final Histogram histogram;
		public static final long MAX_LATENCY_VALUE = TimeUnit.SECONDS
				.toNanos(1);

		private long startTime;
		private long endTime;
		
		private final boolean isWarmup;

		public PeriodicLatencyHistogram(boolean isWarmup) {

			this.isWarmup = isWarmup;
			histogram = new ConcurrentHistogram(MAX_LATENCY_VALUE*10, 3);
		}

		public void setEndTimeStamp(long now) {
			this.endTime = now;
		}

		public void setStartTimeStamp(long now) {
			this.startTime = now;
		}

		public void recordValue(long value) {
			// avoid java.lang.ArrayIndexOutOfBoundsException: value outside of histogram covered range.
	        // clearly if we observe MAX_LATENCY_VALUE then it is already a bad situation
	        if(value >= MAX_LATENCY_VALUE){
	        	value = MAX_LATENCY_VALUE;
	        }
	        
			getHistogram().recordValue(value);
		}

		public Histogram getHistogram() {
			return histogram;
		}
		
		public boolean isWarmup(){
			return this.isWarmup;
		}
	}
	
	public void reportPercentiles(Histogram histogram, PrintStream output){

		if(histogram == null){
			histogram = getIntervalHistogram().getHistogram();
		}
		
    	double[] percentiles = {
    			1,
    			50,
    			75,
    			87.5,
    			96.875,
    			98.4375,
    			99.21875,
    			99.609375,
    			99.8046875,
    			99.90234375,
    			99.951171875,
    			99.9755859375,
    			99.99,
    			100.0
    			};
    	
    	output.format("%12s %14s %10s %14s\n\n", "Value", "Percentile", "IntervalCount", "1/(1-Percentile)");

    	int numberOfSignificantValueDigits = SIG_DIGITS;
        String percentileFormatString = "%12." + numberOfSignificantValueDigits + "f %2.12f %10d %14.2f\n";
        String lastLinePercentileFormatString = "%12." + numberOfSignificantValueDigits + "f %2.12f %10d\n";
        
        int i=0;
        long lastValue=0;
        while (i<percentiles.length) {
            //HistogramIterationValue iterationValue = iterator.next();
        	double pct = percentiles[i];
        	long value = histogram.getValueAtPercentile(pct);
        	long count = histogram.getCountBetweenValues(0,value) - 
        			histogram.getCountBetweenValues(0,lastValue);
            if (pct < 100.0D) {
            	output.format(Locale.US, percentileFormatString,
            			value / HISTOGRAM_SCALING_RATIO,
                        pct/100.0D,
                        count,
                        1/(1.0D - (pct/100.0D)) );
            } else {
            	output.format(Locale.US, lastLinePercentileFormatString,
                        value / HISTOGRAM_SCALING_RATIO,
                        pct/100.0D,
                        count);
            }
            i++;
            lastValue = value;
        }
        
        double mean =  histogram.getMean() / HISTOGRAM_SCALING_RATIO;
        double std_deviation = histogram.getStdDeviation() / HISTOGRAM_SCALING_RATIO;
        output.format(Locale.US,
                "#[Mean    = %12." + numberOfSignificantValueDigits + "f, StdDeviation   = %12." +
                        numberOfSignificantValueDigits +"f]\n",
                mean, std_deviation);
        output.format(Locale.US,
                "#[Max     = %12." + numberOfSignificantValueDigits + "f, Total count    = %12d]\n",
                histogram.getMaxValue() / HISTOGRAM_SCALING_RATIO, histogram.getTotalCount());
	}

	public void report(PrintStream printStream) {
		reportPercentiles(null, printStream);
	}

	public void warmupComplete() {
		this.isWarmup = false;
	}

}
