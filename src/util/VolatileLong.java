package util;

public final class VolatileLong {
    private static final long valueOffset;

    static {
        try {
            valueOffset = UnsafeAccess.unsafe
                    .objectFieldOffset(VolatileLong.class
                            .getDeclaredField("value"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private long value;

    public VolatileLong() {
    }

    public VolatileLong(final long initialValue) {
        lazySet(initialValue);
    }

    public long volatileGet() {
        return UnsafeAccess.unsafe.getLongVolatile(this, valueOffset);
    }

    public long plainGet() {
        return value;
    }

    public void lazyInc() {
        lazySet(value + 1);
    }

    public void lazyInc(long d) {
        lazySet(value + d);
    }

    public void volatileSet(final long value) {
        UnsafeAccess.unsafe.putLongVolatile(this, valueOffset, value);
    }

    public void lazySet(final long value) {
        UnsafeAccess.unsafe.putOrderedLong(this, valueOffset, value);
    }

    public void plainSet(final long value) {
        this.value = value;
    }

    public boolean compareAndSet(final long expectedValue, final long newValue) {
        return UnsafeAccess.unsafe.compareAndSwapLong(this, valueOffset,
                expectedValue, newValue);
    }

    public String toString() {
        return Long.toString(volatileGet());
    }
}
