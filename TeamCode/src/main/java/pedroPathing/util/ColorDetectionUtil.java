package pedroPathing.util;

import com.qualcomm.hardware.rev.RevColorSensorV3;
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;
import pedroPathing.constants.ControlConstants;
import pedroPathing.hardware.SampleColor;

/**
 * Unified color detection utility for both intake and outtake systems.
 * Provides consistent, optimized color detection with caching and performance improvements.
 * 
 * Features:
 * - Consistent RGB ranges across all systems
 * - Configurable caching to reduce I2C calls
 * - Batch sensor reads for better performance
 * - Distance-based filtering
 * - Debug support with raw values
 * 
 * @author Baron Henderson - 20077 The Indubitables
 * @version 1.0, 12/30/2024
 */
public class ColorDetectionUtil {
    
    // Enhanced cached color data with adaptive performance optimization
    private static class ColorCache {
        SampleColor color = SampleColor.NONE;
        long lastReadTime = 0;
        double[] lastRawValues = new double[4]; // R, G, B, Distance

        // Adaptive caching: faster reads when sample detected, slower when none
        boolean samplePresent = false;
        long adaptiveInterval = ControlConstants.COLOR_READ_INTERVAL_MS;
    }
    
    // Cache instances for different sensors (intake vs outtake)
    private final ColorCache cache = new ColorCache();
    private final RevColorSensorV3 colorSensor;
    private final String sensorName; // For debugging/telemetry

    // Reusable array to reduce garbage collection pressure
    private final double[] reusableRgbArray = new double[4];
    
    /**
     * Create a color detection utility for a specific sensor
     * @param colorSensor The RevColorSensorV3 to use
     * @param sensorName Name for debugging (e.g., "intake", "outtake")
     */
    public ColorDetectionUtil(RevColorSensorV3 colorSensor, String sensorName) {
        this.colorSensor = colorSensor;
        this.sensorName = sensorName;
    }
    
    /**
     * Get the detected sample color with caching optimization
     * @return The detected SampleColor
     */
    public SampleColor getSampleColor() {
        if (colorSensor == null) return SampleColor.NONE;

        // Adaptive performance optimization: Faster reads when sample detected, slower when none
        long currentTime = System.currentTimeMillis();
        if (currentTime - cache.lastReadTime < cache.adaptiveInterval) {
            return cache.color;
        }
        cache.lastReadTime = currentTime;
        
        // Batch all color sensor reads to benefit from bulk reading
        double red = colorSensor.red();
        double green = colorSensor.green();
        double blue = colorSensor.blue();
        double distance = colorSensor.getDistance(DistanceUnit.CM);
        
        // Store raw values for debugging
        cache.lastRawValues[0] = red;
        cache.lastRawValues[1] = green;
        cache.lastRawValues[2] = blue;
        cache.lastRawValues[3] = distance;
        
        // Only check for color if the sensor is close enough
        if (distance > ControlConstants.COLOR_DETECTION_MAX_DISTANCE_CM) {
            cache.color = SampleColor.NONE;
            cache.samplePresent = false;
            // Slower reads when no sample present (reduce unnecessary I2C calls)
            cache.adaptiveInterval = ControlConstants.COLOR_READ_SLOW_INTERVAL_MS;
            return cache.color;
        }

        // Unified color detection logic using constants
        cache.color = detectColorFromRGB(red, green, blue);
        cache.samplePresent = (cache.color != SampleColor.NONE);

        // Adaptive interval: faster when sample detected for responsiveness
        if (cache.samplePresent) {
            cache.adaptiveInterval = ControlConstants.COLOR_READ_FAST_INTERVAL_MS; // 25ms when sample present
        } else {
            cache.adaptiveInterval = ControlConstants.COLOR_READ_INTERVAL_MS; // 50ms when close but no color
        }

        return cache.color;
    }
    
    /**
     * Detect color from RGB values using unified constants with optimized early-exit logic
     * @param red Red value
     * @param green Green value
     * @param blue Blue value
     * @return Detected SampleColor
     */
    private SampleColor detectColorFromRGB(double red, double green, double blue) {
        // Performance optimization: Check most distinctive channel first for early exit

        // Blue detection - check blue channel first (most distinctive)
        if (blue >= ControlConstants.BLUE_B_MIN && blue <= ControlConstants.BLUE_B_MAX) {
            if (isInRange(red, ControlConstants.BLUE_R_MIN, ControlConstants.BLUE_R_MAX) &&
                isInRange(green, ControlConstants.BLUE_G_MIN, ControlConstants.BLUE_G_MAX)) {
                return SampleColor.BLUE;
            }
        }

        // Yellow detection - check green channel first (most distinctive for yellow)
        if (green >= ControlConstants.YELLOW_G_MIN && green <= ControlConstants.YELLOW_G_MAX) {
            if (isInRange(red, ControlConstants.YELLOW_R_MIN, ControlConstants.YELLOW_R_MAX) &&
                isInRange(blue, ControlConstants.YELLOW_B_MIN, ControlConstants.YELLOW_B_MAX)) {
                return SampleColor.YELLOW;
            }
        }

        // Red detection - check red channel first (most distinctive)
        if (red >= ControlConstants.RED_R_MIN && red <= ControlConstants.RED_R_MAX) {
            if (isInRange(green, ControlConstants.RED_G_MIN, ControlConstants.RED_G_MAX) &&
                isInRange(blue, ControlConstants.RED_B_MIN, ControlConstants.RED_B_MAX)) {
                return SampleColor.RED;
            }
        }

        return SampleColor.NONE;
    }
    
    /**
     * Helper method to check if a value is within a range (inclusive)
     */
    private boolean isInRange(double value, double min, double max) {
        return value >= min && value <= max;
    }
    
    /**
     * Check if a sample is detected (any color)
     * @return true if any sample color is detected
     */
    public boolean isSamplePresent() {
        return getSampleColor() != SampleColor.NONE;
    }
    
    /**
     * Check if a sample is detected based on distance only (fastest check)
     * @return true if sensor detects something within detection range
     */
    public boolean isSampleDetected() {
        if (colorSensor == null) return false;
        return colorSensor.getDistance(DistanceUnit.CM) < ControlConstants.SAMPLE_DETECTION_DISTANCE_CM;
    }

    /**
     * Get just the distance reading for ultra-fast sample presence detection
     * This is the fastest possible check - only one I2C call
     * @return Distance in CM, or -1 if sensor unavailable
     */
    public double getDistance() {
        if (colorSensor == null) return -1;
        return colorSensor.getDistance(DistanceUnit.CM);
    }
    
    /**
     * Get raw RGB and distance values for debugging
     * Only call when needed to avoid performance impact
     * @return Array of [red, green, blue, distance] - DO NOT MODIFY the returned array
     */
    public double[] getRawRGBValues() {
        if (colorSensor == null) {
            reusableRgbArray[0] = 0;
            reusableRgbArray[1] = 0;
            reusableRgbArray[2] = 0;
            reusableRgbArray[3] = 0;
            return reusableRgbArray;
        }

        // Return cached values if recent, otherwise read fresh
        long currentTime = System.currentTimeMillis();
        if (currentTime - cache.lastReadTime < ControlConstants.COLOR_READ_INTERVAL_MS) {
            // Copy cached values to reusable array to avoid allocation
            System.arraycopy(cache.lastRawValues, 0, reusableRgbArray, 0, 4);
            return reusableRgbArray;
        }

        // Read fresh values into reusable array
        reusableRgbArray[0] = colorSensor.red();
        reusableRgbArray[1] = colorSensor.green();
        reusableRgbArray[2] = colorSensor.blue();
        reusableRgbArray[3] = colorSensor.getDistance(DistanceUnit.CM);

        return reusableRgbArray;
    }
    
    /**
     * Force a fresh color reading, bypassing cache
     * Use sparingly as this impacts performance
     * @return Fresh SampleColor reading
     */
    public SampleColor getFreshSampleColor() {
        cache.lastReadTime = 0; // Force cache miss
        return getSampleColor();
    }
    
    /**
     * Get the sensor name for debugging
     * @return Sensor name
     */
    public String getSensorName() {
        return sensorName;
    }
    
    /**
     * Check if the color sensor is available
     * @return true if sensor is not null
     */
    public boolean isSensorAvailable() {
        return colorSensor != null;
    }
}
