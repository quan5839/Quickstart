package pedroPathing.util;

import pedroPathing.constants.ControlConstants;

/**
 * Lightweight performance monitoring utility for FTC robot loops
 * Tracks loop times, percentiles, and provides performance insights
 * 
 * @author Baron Henderson - 20077 The Indubitables
 * @version 1.0, 12/30/2024
 */
public class PerformanceMonitor {
    
    // Circular buffer for loop times (more memory efficient than ArrayList)
    private final double[] loopTimes;
    private final int bufferSize;
    private int currentIndex = 0;
    private boolean bufferFull = false;
    
    // Cached metrics to avoid recalculation
    private double cachedAverage = 0.0;
    private double cachedOnePercentLow = 0.0;
    private double cachedMedian = 0.0;
    private long lastCalculationTime = 0;
    private static final long CALCULATION_CACHE_MS = 100; // Cache metrics for 100ms
    
    // Temporary array for sorting (reused to reduce allocations)
    private final double[] sortBuffer;
    
    public PerformanceMonitor() {
        this(ControlConstants.PERCENTILE_BUFFER_SIZE);
    }
    
    public PerformanceMonitor(int bufferSize) {
        this.bufferSize = bufferSize;
        this.loopTimes = new double[bufferSize];
        this.sortBuffer = new double[bufferSize];
    }
    
    /**
     * Record a new loop time measurement
     * @param loopTimeMs Loop time in milliseconds
     */
    public void recordLoopTime(double loopTimeMs) {
        loopTimes[currentIndex] = loopTimeMs;
        currentIndex = (currentIndex + 1) % bufferSize;
        
        if (!bufferFull && currentIndex == 0) {
            bufferFull = true;
        }
        
        // Invalidate cached metrics
        lastCalculationTime = 0;
    }
    
    /**
     * Get the average loop time
     * @return Average loop time in milliseconds
     */
    public double getAverageLoopTime() {
        calculateMetricsIfNeeded();
        return cachedAverage;
    }
    
    /**
     * Get the 1% low (worst 1% of loop times)
     * @return 1% low loop time in milliseconds
     */
    public double getOnePercentLow() {
        calculateMetricsIfNeeded();
        return cachedOnePercentLow;
    }
    
    /**
     * Get the median loop time
     * @return Median loop time in milliseconds
     */
    public double getMedianLoopTime() {
        calculateMetricsIfNeeded();
        return cachedMedian;
    }
    
    /**
     * Check if we have enough data for meaningful metrics
     * @return true if buffer has at least 10 samples
     */
    public boolean hasEnoughData() {
        return bufferFull || currentIndex >= 10;
    }
    
    /**
     * Get the number of samples currently stored
     * @return Number of samples
     */
    public int getSampleCount() {
        return bufferFull ? bufferSize : currentIndex;
    }
    
    /**
     * Calculate metrics only when needed and cache results
     */
    private void calculateMetricsIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCalculationTime < CALCULATION_CACHE_MS) {
            return; // Use cached values
        }
        
        if (!hasEnoughData()) {
            cachedAverage = 0.0;
            cachedOnePercentLow = 0.0;
            cachedMedian = 0.0;
            return;
        }
        
        int sampleCount = getSampleCount();
        
        // Copy data to sort buffer
        if (bufferFull) {
            System.arraycopy(loopTimes, 0, sortBuffer, 0, bufferSize);
        } else {
            System.arraycopy(loopTimes, 0, sortBuffer, 0, currentIndex);
        }
        
        // Sort for percentile calculations
        java.util.Arrays.sort(sortBuffer, 0, sampleCount);
        
        // Calculate average
        double sum = 0;
        for (int i = 0; i < sampleCount; i++) {
            sum += sortBuffer[i];
        }
        cachedAverage = sum / sampleCount;
        
        // Calculate median
        if (sampleCount % 2 == 0) {
            cachedMedian = (sortBuffer[sampleCount / 2 - 1] + sortBuffer[sampleCount / 2]) / 2.0;
        } else {
            cachedMedian = sortBuffer[sampleCount / 2];
        }
        
        // Calculate 1% low (99th percentile)
        int onePercentIndex = Math.max(0, (int) (sampleCount * 0.99) - 1);
        cachedOnePercentLow = sortBuffer[onePercentIndex];
        
        lastCalculationTime = currentTime;
    }
    
    /**
     * Reset all performance data
     */
    public void reset() {
        currentIndex = 0;
        bufferFull = false;
        lastCalculationTime = 0;
        cachedAverage = 0.0;
        cachedOnePercentLow = 0.0;
        cachedMedian = 0.0;
    }
    
    /**
     * Get a performance summary string for telemetry
     * @return Formatted performance string
     */
    public String getPerformanceSummary() {
        if (!hasEnoughData()) {
            return "Collecting data...";
        }
        
        return String.format("Avg: %.1fms | Med: %.1fms | 1%%Low: %.1fms", 
                           getAverageLoopTime(), getMedianLoopTime(), getOnePercentLow());
    }
    
    /**
     * Check if performance is within acceptable ranges
     * @param maxAverageMs Maximum acceptable average loop time
     * @param maxOnePercentMs Maximum acceptable 1% low time
     * @return true if performance is acceptable
     */
    public boolean isPerformanceAcceptable(double maxAverageMs, double maxOnePercentMs) {
        if (!hasEnoughData()) return true; // Not enough data to judge
        
        return getAverageLoopTime() <= maxAverageMs && getOnePercentLow() <= maxOnePercentMs;
    }
}
