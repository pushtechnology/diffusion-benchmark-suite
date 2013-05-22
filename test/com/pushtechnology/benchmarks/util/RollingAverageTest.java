package com.pushtechnology.benchmarks.util;

import static org.junit.Assert.*;

import org.junit.Test;

import com.pushtechnology.benchmarks.util.RollingAverage;

public class RollingAverageTest {

    @Test
    public void test() {
        RollingAverage avg = new RollingAverage(4);
        assertEquals(0, avg.avg());
        avg.sample(1);
        assertEquals(1, avg.avg());
        avg.sample(1);
        assertEquals(1, avg.avg());
        avg.sample(1);
        assertEquals(1, avg.avg());
        avg.sample(1);
        assertEquals(1, avg.avg());
        avg.sample(1);
        assertEquals(1, avg.avg());
        avg.sample(5);
        assertEquals(2, avg.avg());
        avg.sample(5);
        assertEquals(3, avg.avg());
        avg.sample(5);
        assertEquals(4, avg.avg());
        avg.sample(5);
        assertEquals(5, avg.avg());
    }

}
