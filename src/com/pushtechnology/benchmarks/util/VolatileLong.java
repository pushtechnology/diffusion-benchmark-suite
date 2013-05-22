/*
 * Copyright 2013 Push Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.pushtechnology.benchmarks.util;

/**
 * A long wrapper allowing control over memory access method.
 * 
 * @author nitsanw
 *
 */
public final class VolatileLong {
    /** memory offset to support unsafe access. */
    private static final long VALUE_OFFSET;

    static {
        try {
            VALUE_OFFSET = UnsafeAccess.UNSAFE
                    .objectFieldOffset(VolatileLong.class
                            .getDeclaredField("value"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** actual value, plain. */
    private long value;

    /**
     * initial value is 0L.
     */
    public VolatileLong() {
        // nothing to do, java inits all memory to 0.
    }

    /**
     * construct initialized to.
     * 
     * @param initialValue to set to
     */
    public VolatileLong(long initialValue) {
        lazySet(initialValue);
    }

    /**
     * @return volatile read of value.
     */
    public long volatileGet() {
        return UnsafeAccess.UNSAFE.getLongVolatile(this, VALUE_OFFSET);
    }

    /**
     * @return plain read of value.
     */
    public long plainGet() {
        return value;
    }

    /**
     * increment value and lazySet it.
     */
    public void lazyInc() {
        lazySet(value + 1);
    }

    /**
     * increment value by d and lazySet it.
     * @param d ...
     */
    public void lazyAdd(long d) {
        lazySet(value + d);
    }

    /**
     * @param newVal to be written using a volatile write(STORE/LOAD).
     */
    public void volatileSet(long newVal) {
        UnsafeAccess.UNSAFE.putLongVolatile(this, VALUE_OFFSET, newVal);
    }
    /**
     * @param newVal to be written using a ordered write(STORE/STORE).
     */
    public void lazySet(long newVal) {
        UnsafeAccess.UNSAFE.putOrderedLong(this, VALUE_OFFSET, newVal);
    }
    /**
     * @param newVal to be written using a plain write.
     */
    public void plainSet(long newVal) {
        this.value = newVal;
    }

    /**
     * @param expectedValue ...
     * @param newValue ...
     * @return true is CAS success, false otherwise
     */
    public boolean compareAndSet(long expectedValue, long newValue) {
        return UnsafeAccess.UNSAFE.compareAndSwapLong(this, VALUE_OFFSET,
                expectedValue, newValue);
    }

    /**
     * @return String.valueOf(value)
     */
    public String toString() {
        return Long.toString(volatileGet());
    }
}
