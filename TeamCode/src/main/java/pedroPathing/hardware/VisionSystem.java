package pedroPathing.hardware;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.RobotLog;

import org.json.JSONException;
import org.json.JSONObject;

import pedroPathing.hardware.SampleColor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * FatDragon-Based VisionSystem class for interfacing with Limelight 3A
 * This class provides methods to communicate with a Limelight 3A using proven FatDragon algorithms
 * Uses proper Limelight3A SDK integration with FatDragon's reliable sample detection
 *
 * Based on proven working algorithms from FTC team 12527 FatDragon
 */
public class VisionSystem {
    private final OpMode opMode;
    private Limelight3A limelight;

    // Limelight network configuration for HTTP commands
    private static final String LIMELIGHT_URL = "http://limelight.local:5807";
    private static final String RESULTS_PATH = "/results";

    // Cache for vision data
    private JSONObject lastResults;
    private long lastUpdateTime = 0;
    private static final long UPDATE_THRESHOLD_MS = 100; // Limit update rate to 10Hz

    // Vision data
    private boolean targetVisible = false;
    private double tx = 0.0; // Horizontal offset from crosshair to target (-29.8 to 29.8 degrees)
    private double ty = 0.0; // Vertical offset from crosshair to target (-24.85 to 24.85 degrees)
    private double ta = 0.0; // Target area (0% to 100% of image)

    // Enhanced color recognition data (from Python scripts)
    private int samplesDetected = 0;
    private double sampleX = 0.0;
    private double sampleY = 0.0;
    private double sampleAngle = 0.0;
    private SampleColor detectedColor = SampleColor.NONE;
    private int quadrant = 0; // 0=none, 1=top, 2=bottom
    private int confidence = 0; // 0-100 confidence percentage
    private int sampleArea = 0; // Sample area in pixels
    private boolean colorRecognitionEnabled = false;

    // Performance tracking
    private long lastVisionProcessTime = 0;
    private double avgProcessingTime = 0.0;

    /**
     * Constructor for VisionSystem
     *
     * @param opMode Reference to the OpMode
     */
    public VisionSystem(OpMode opMode) {
        this.opMode = opMode;
    }

    /**
     * Initialize the Limelight 3A vision system
     */
    public void init() {
        try {
            // Initialize Limelight 3A from hardware map
            limelight = opMode.hardwareMap.get(Limelight3A.class, "limelight");

            // Set telemetry transmission interval for better performance
            opMode.telemetry.setMsTransmissionInterval(50);

            // Start with FatDragon pipeline (0) - FatDragon script is uploaded to Pipeline 0
            limelight.pipelineSwitch(0);

            // Start polling for data
            limelight.start();

            opMode.telemetry.addData("Vision System", "✅ Limelight 3A Initialized");
            RobotLog.dd("VisionSystem", "Limelight 3A initialized successfully");
        } catch (Exception e) {
            opMode.telemetry.addData("Vision System Error", "❌ " + e.getMessage());
            RobotLog.ee("VisionSystem", e, "Error initializing Limelight 3A");
        }
    }

    /**
     * Update vision data from Limelight 3A
     *
     * @return true if update was successful
     */
    public boolean update() {
        long currentTime = System.currentTimeMillis();

        // Limit update rate to reduce processing load
        if (currentTime - lastUpdateTime < UPDATE_THRESHOLD_MS) {
            return false;
        }

        try {
            if (limelight == null) {
                return false;
            }

            // Get latest result from Limelight 3A
            LLResult result = limelight.getLatestResult();
            if (result != null) {
                parseLimelightResult(result);
                lastUpdateTime = currentTime;
                return true;
            }
        } catch (Exception e) {
            opMode.telemetry.addData("Limelight Error", e.getMessage());
            RobotLog.ee("VisionSystem", e, "Error updating vision data");
        }

        return false;
    }

    /**
     * Parse vision data from Limelight 3A result
     *
     * @param result LLResult from Limelight 3A
     */
    private void parseLimelightResult(LLResult result) {
        try {
            // Parse basic targeting data
            targetVisible = result.isValid();

            if (targetVisible) {
                tx = result.getTx();
                ty = result.getTy();
                ta = result.getTa();
            } else {
                tx = 0.0;
                ty = 0.0;
                ta = 0.0;
            }

            // Parse enhanced Python data if color recognition is enabled
            if (colorRecognitionEnabled) {
                parseEnhancedPythonData(result);
            }

        } catch (Exception e) {
            opMode.telemetry.addData("Parse Error", e.getMessage());
            RobotLog.ee("VisionSystem", e, "Error parsing Limelight result");
        }
    }

    /**
     * Parse enhanced Python data from FatDragon-based Limelight 3A result
     * Compatible with both original and FatDragon implementations
     *
     * FatDragon format: [samples_detected, x, y, angle, color_code, quadrant, confidence, distance]
     * - Uses 256x144 processing resolution
     * - Index 7 contains distance in inches (stored in sampleArea field)
     * - Higher confidence thresholds (70%+)
     *
     * @param result LLResult containing Python output
     */
    private void parseEnhancedPythonData(LLResult result) {
        try {
            // Get Python output array from Limelight result
            double[] pythonOutput = result.getPythonOutput();

            if (pythonOutput != null && pythonOutput.length >= 8) {
                samplesDetected = (int) pythonOutput[0];
                sampleX = pythonOutput[1];
                sampleY = pythonOutput[2];
                sampleAngle = pythonOutput[3];
                int colorCode = (int) pythonOutput[4];
                quadrant = (int) pythonOutput[5];
                confidence = (int) pythonOutput[6];
                sampleArea = (int) pythonOutput[7];

                // Convert color code to SampleColor enum
                switch (colorCode) {
                    case 1:
                        detectedColor = SampleColor.RED;
                        break;
                    case 2:
                        detectedColor = SampleColor.YELLOW;
                        break;
                    case 3:
                        detectedColor = SampleColor.BLUE;
                        break;
                    default:
                        detectedColor = SampleColor.NONE;
                        break;
                }

                // Track processing performance
                long currentTime = System.currentTimeMillis();
                if (lastVisionProcessTime > 0) {
                    long processingTime = currentTime - lastVisionProcessTime;
                    avgProcessingTime = avgProcessingTime * 0.9 + processingTime * 0.1; // Exponential moving average
                }
                lastVisionProcessTime = currentTime;
            } else {
                // No valid Python data - reset sample detection
                samplesDetected = 0;
                detectedColor = SampleColor.NONE;
                confidence = 0;
            }

        } catch (Exception e) {
            opMode.telemetry.addData("Python Parse Error", e.getMessage());
            RobotLog.ee("VisionSystem", e, "Error parsing Python output");
        }
    }



    /**
     * Check if target is currently visible
     *
     * @return true if target is detected
     */
    public boolean isTargetVisible() {
        return targetVisible;
    }

    /**
     * Get horizontal offset to target in degrees
     *
     * @return Horizontal angle to target (-29.8 to 29.8)
     */
    public double getTargetX() {
        return tx;
    }

    /**
     * Get vertical offset to target in degrees
     *
     * @return Vertical angle to target (-24.85 to 24.85)
     */
    public double getTargetY() {
        return ty;
    }

    /**
     * Get target area as percentage of image
     *
     * @return Target area (0 to 100)
     */
    public double getTargetArea() {
        return ta;
    }

    /**
     * Calculate approximate distance to target using target height
     *
     * @param targetHeightInches Real height of target in inches
     * @param cameraHeightInches Camera height from ground in inches
     * @param cameraPitchDegrees Camera pitch in degrees (positive is tilted up)
     * @return Estimated distance to target in inches
     */
    public double getDistanceToTarget(double targetHeightInches, double cameraHeightInches, double cameraPitchDegrees) {
        if (!targetVisible) return -1.0;

        // Distance calculation using target height and vertical angle
        double targetAngleRadians = Math.toRadians(ty + cameraPitchDegrees);

        return (targetHeightInches - cameraHeightInches) / Math.tan(targetAngleRadians);
    }

    /**
     * Set Limelight LED mode
     *
     * @param mode 0=current pipeline setting, 1=force off, 2=force blink, 3=force on
     */
    public void setLedMode(int mode) {
        sendNetworkTableCommand("ledMode", mode);
    }

    /**
     * Set Limelight processing pipeline
     *
     * @param pipeline Pipeline index (0-9)
     */
    public void setPipeline(int pipeline) {
        if (limelight != null) {
            limelight.pipelineSwitch(pipeline);
        }
    }

    /**
     * Send a command to the Limelight NetworkTables
     *
     * @param key   NetworkTable key
     * @param value Value to set
     */
    private void sendNetworkTableCommand(String key, Object value) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(LIMELIGHT_URL + "/table/" + key + "/" + value);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(100);
            connection.setReadTimeout(100);

            connection.getResponseCode(); // Send the request
        } catch (IOException e) {
            opMode.telemetry.addData("Limelight Command Error", e.getMessage());
            RobotLog.ee("VisionSystem", e, "Error sending command to Limelight");
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Release resources
     */
    public void shutdown() {
        if (limelight != null) {
            limelight.stop();
        }
    }

    // ========== COLOR RECOGNITION METHODS ==========

    /**
     * Enable or disable FatDragon color recognition processing
     * When enabled, the system will parse color data from FatDragon Python scripts
     *
     * @param enabled true to enable FatDragon color recognition, false to disable
     */
    public void setColorRecognitionEnabled(boolean enabled) {
        this.colorRecognitionEnabled = enabled;
        if (enabled) {
            // Switch to FatDragon detection pipeline (pipeline 0)
            setPipeline(0);
        } else {
            // Keep using pipeline 0 (FatDragon handles both detection and basic targeting)
            setPipeline(0);
        }
    }

    /**
     * Check if color recognition is currently enabled
     *
     * @return true if color recognition is enabled
     */
    public boolean isColorRecognitionEnabled() {
        return colorRecognitionEnabled;
    }

    /**
     * Get the number of samples detected by color recognition
     *
     * @return number of samples detected (0 if none)
     */
    public int getSamplesDetected() {
        return samplesDetected;
    }

    /**
     * Get the X coordinate of the detected sample
     *
     * @return X coordinate in pixels, or 0 if no sample detected
     */
    public double getSampleX() {
        return sampleX;
    }

    /**
     * Get the Y coordinate of the detected sample
     *
     * @return Y coordinate in pixels, or 0 if no sample detected
     */
    public double getSampleY() {
        return sampleY;
    }

    /**
     * Get the angle/orientation of the detected sample
     *
     * @return angle in degrees (0-360), or 0 if no sample detected
     */
    public double getSampleAngle() {
        return sampleAngle;
    }

    /**
     * Get the confidence score of the color detection
     *
     * @return confidence percentage (0-100), or 0 if no sample detected
     */
    public int getConfidence() {
        return confidence;
    }

    /**
     * Get the area of the detected sample
     * Note: When using FatDragon implementation, this returns distance in inches
     *
     * @return area in pixels (or distance in inches for FatDragon), or 0 if no sample detected
     */
    public int getSampleArea() {
        return sampleArea;
    }

    /**
     * Get the estimated distance to the detected sample (FatDragon implementation)
     * This is the same as getSampleArea() when using FatDragon pipeline
     *
     * @return distance in inches, or 0 if no sample detected
     */
    public int getSampleDistance() {
        return sampleArea; // FatDragon stores distance in sampleArea field
    }

    /**
     * Get the color of the detected sample
     *
     * @return SampleColor enum (RED, BLUE, YELLOW, or NONE)
     */
    public SampleColor getDetectedColor() {
        return detectedColor;
    }

    /**
     * Get the quadrant where the sample was detected
     *
     * @return quadrant number (0=none, 1=top, 2=bottom)
     */
    public int getQuadrant() {
        return quadrant;
    }

    /**
     * Check if a sample is detected in the top quadrant
     *
     * @return true if sample detected in top quadrant
     */
    public boolean isSampleInTopQuadrant() {
        return quadrant == 1 && samplesDetected > 0;
    }

    /**
     * Check if a sample is detected in the bottom quadrant
     *
     * @return true if sample detected in bottom quadrant
     */
    public boolean isSampleInBottomQuadrant() {
        return quadrant == 2 && samplesDetected > 0;
    }

    /**
     * Check if any sample is detected by color recognition
     *
     * @return true if at least one sample is detected
     */
    public boolean isColorSampleDetected() {
        return samplesDetected > 0 && detectedColor != SampleColor.NONE;
    }

    /**
     * Check if the detected sample has high confidence
     *
     * @param minConfidence Minimum confidence threshold (0-100)
     * @return true if confidence is above threshold
     */
    public boolean isHighConfidenceDetection(int minConfidence) {
        return confidence >= minConfidence && samplesDetected > 0;
    }

    /**
     * Get the average vision processing time
     *
     * @return average processing time in milliseconds
     */
    public double getAverageProcessingTime() {
        return avgProcessingTime;
    }

    /**
     * Get a formatted string with all FatDragon color recognition data
     * Useful for telemetry display
     *
     * @return formatted string with FatDragon color recognition info
     */
    public String getColorRecognitionSummary() {
        if (!colorRecognitionEnabled) {
            return "FatDragon Vision: DISABLED";
        }

        if (samplesDetected == 0) {
            return "FatDragon Vision: No samples detected";
        }

        String quadrantText = quadrant == 1 ? "TOP" : quadrant == 2 ? "BOTTOM" : "CENTER";
        return String.format("Samples: %d | Color: %s | Pos: (%.0f,%.0f) | Angle: %.1f° | Q: %s | Conf: %d%% | Dist: %d\"",
                           samplesDetected, detectedColor, sampleX, sampleY, sampleAngle, quadrantText, confidence, sampleArea);
    }

    /**
     * Get a detailed performance summary
     *
     * @return formatted string with performance metrics
     */
    public String getPerformanceSummary() {
        return String.format("Vision Performance: Avg: %.1fms | Last Update: %dms ago",
                           avgProcessingTime, System.currentTimeMillis() - lastUpdateTime);
    }
}