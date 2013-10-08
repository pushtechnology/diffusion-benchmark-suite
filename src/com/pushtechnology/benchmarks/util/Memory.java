package com.pushtechnology.benchmarks.util;

/**
 * Utility class for handling memory.
 * @author mchampion - created 8 Oct 2013
 */
public final class Memory {
    // TODO: Provide support for other representations of memory
    /**
     * The default decimal precision to display memory.
     */
    public static final int DEFAULT_PRECISION = 4;
    /**
     * Bytes in megabytes.
     */
    private static final long BYTES_IN_MB = 1024L * 1024L;

    /**
     * Private constructor.
     */
    private Memory() {
    }

    /**
     * Format bytes to megabytes. Using default decimal places.
     *
     * @param bytes Number of bytes
     * @return Formatted string
     */
    public static String formatMemory(long bytes) {
        return formatMemory(bytes, DEFAULT_PRECISION);
    }

    /**
     * Format bytes to megabytes.
     *
     * @param bytes Number of bytes
     * @param precision Number of decimal places
     * @return Formatted string
     */
    public static String formatMemory(long bytes, int precision) {
        return String.format("%." + precision + "f",
                ((double) bytes / (double) BYTES_IN_MB));
    }
}
