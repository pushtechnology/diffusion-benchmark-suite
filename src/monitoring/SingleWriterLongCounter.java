package monitoring;

import java.lang.ref.WeakReference;
import java.util.concurrent.CopyOnWriteArrayList;

import util.VolatileLong;

public class SingleWriterLongCounter {
    private final CopyOnWriteArrayList<WeakReference<VolatileLong>> counters =
            new CopyOnWriteArrayList<WeakReference<VolatileLong>>();


    ThreadLocal<VolatileLong> tlc = new ThreadLocal<VolatileLong>() {
        @Override
        protected VolatileLong initialValue() {
            VolatileLong lc = new VolatileLong();
            counters.add(new WeakReference<VolatileLong>(lc));
            return lc;
        }
    };

    public void inc() {
        tlc.get().lazyInc();
    }

    public void inc(long d) {
        tlc.get().lazyInc(d);
    }

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

    /**
     * @param args
     */
    public static void main(String[] args) {
    }

}
