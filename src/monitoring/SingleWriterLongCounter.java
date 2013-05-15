package monitoring;

import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;

import util.VolatileLong;

/**
 * A single writer counter implemented via a set of thread local counters being
 * written to and an aggregate view acting as the sum value.
 * 
 * @author nitsanw
 * 
 */
public final class SingleWriterLongCounter {
    /**
     * COW used for counters collection as written to from monitored threads and
     * read from the monitoring threads. Weak references are used to prevent
     * memory leak when threads are churned, this leads to the possibility
     * of having the counter value go down as thread local values disappear.
     * May require further thought.
     */
    private final CopyOnWriteArrayList<WeakReference<VolatileLong>> counters =
            new CopyOnWriteArrayList<WeakReference<VolatileLong>>();

    /** thread local counter registered with collection above. */
    private final ThreadLocal<VolatileLong> tlc = 
            new ThreadLocal<VolatileLong>() {
        @Override
        protected VolatileLong initialValue() {
            VolatileLong lc = new VolatileLong();
            counters.add(new WeakReference<VolatileLong>(lc));
            return lc;
        }
    };

    /**
     * increase counter by one.
     */
    public void inc() {
        tlc.get().lazyInc();
    }

    /**
     * increase value by d.
     * @param d ...
     */
    public void add(long d) {
        tlc.get().lazyAdd(d);
    }

    /**
     * @return aggregate value of thread local counters
     */
    public long get() {
        long sum = 0;
        for (WeakReference<VolatileLong> lcRef : counters) {
            VolatileLong lc = lcRef.get();
            if (lc == null) {
                counters.remove(lcRef);
            } else {
                sum += lc.volatileGet();
            }
        }
        return sum;
    }
}
