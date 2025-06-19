package pedroPathing.constants;

import com.acmerobotics.dashboard.config.Config;

/**
 * Vision System Constants for Limelight 3A Camera
 * Organized alphabetically within sections for easy maintenance
 */
@Config
public class VisionConstants {

    // ========== CAMERA HARDWARE SETTINGS ==========

    /** Limelight 3A black level offset */
    public static final int CAMERA_BLACK_LEVEL_OFFSET = 4;

    /** Limelight 3A blue balance */
    public static final int CAMERA_BLUE_BALANCE = 1637;

    /** Limelight 3A camera exposure setting (.01 ms units) - Current: 3300 = 33.00 ms */
    public static final int CAMERA_EXPOSURE = 3300;

    /** Camera frame rate (fps) */
    public static final int CAMERA_FPS = 90;

    /** Limelight 3A horizontal field of view (degrees) - Fixed specification */
    public static final double CAMERA_FOV_HORIZONTAL = 54.5;

    /** Limelight 3A vertical field of view (degrees) - Fixed specification */
    public static final double CAMERA_FOV_VERTICAL = 42;

    /** Height of the Limelight 3A camera above the ground (inches) */
    public static final double CAMERA_HEIGHT = 12.5;

    /** Camera resolution height (pixels) - Must match Limelight 3A configuration */
    public static final int CAMERA_HEIGHT_PIXELS = 240;

    /** Camera offset from robot center - forward/backward direction (inches) */
    public static double CAMERA_OFFSET_X = 2;

    /** Camera offset from robot center - left/right direction (inches) */
    public static double CAMERA_OFFSET_Y = 3.5;

    /** Limelight 3A red balance */
    public static final int CAMERA_RED_BALANCE = 1802;

    /** Limelight 3A sensor gain */
    public static final int CAMERA_SENSOR_GAIN = 19;

    /** Camera tilt angle downward from horizontal (degrees) */
    public static final double CAMERA_TILT_ANGLE = 25.0;

    /** Camera resolution width (pixels) - Must match Limelight 3A configuration */
    public static final int CAMERA_WIDTH = 320;
    // ========== DISTANCE CALCULATION ==========

    /** Default distance when calculation fails (inches) */
    public static final double DEFAULT_DISTANCE = 50.0;

    /** Maximum detectable distance (inches) */
    public static final double MAX_DETECTION_DISTANCE = 150.0;

    /** Maximum age of vision data before considering it stale (ms) */
    public static final int MAX_VISION_DATA_AGE_MS = 200;

    /** Minimum detectable distance (inches) */
    public static final double MIN_DETECTION_DISTANCE = 2.0;

    /** Minimum effective angle for valid distance calculation (degrees) */
    public static final double MIN_EFFECTIVE_ANGLE = 1.0;

    /** Vision processing update rate limit (ms) */
    public static final int VISION_UPDATE_INTERVAL_MS = 50;

    // ========== GAME PIECE SPECIFICATIONS ==========

    /** Approximate diameter of samples (inches) - using length dimension */
    public static final double SAMPLE_DIAMETER = 3.5;

    /** Height of sample CENTER above the ground (inches) */
    public static final double SAMPLE_HEIGHT = 0.75;

    /** FTC sample length (inches) - Official specification */
    public static final double SAMPLE_LENGTH = 3.5;

    /** FTC sample total height (inches) - Official specification */
    public static final double SAMPLE_TOTAL_HEIGHT = 1.5;

    /** FTC sample width (inches) - Official specification */
    public static final double SAMPLE_WIDTH = 1.5;
    // ========== INTAKE CONSTRAINTS ==========


    /** Enable inside area reach detection */
    public static boolean ENABLE_INSIDE_AREA_REACH = true;

    /** Intake base position - forward/backward offset from robot center (inches) */
    public static final double INTAKE_BASE_OFFSET_X = 0.0;

    /** Intake base position - left/right offset from robot center (inches) */
    public static final double INTAKE_BASE_OFFSET_Y = 0.0;

    /** Shoulder/arm reach radius (cm) */
    public static final double INTAKE_SHOULDER_REACH_IN = 3.94;

    /** Maximum slide extension (cm) */
    public static final double INTAKE_SLIDE_MAX_EXTENSION_IN = 15.75;

    /** Turret rotation range (degrees) */
    public static final double INTAKE_TURRET_ROTATION_RANGE = 270.0;

    /** Wrist rotation range (degrees) */
    public static final double INTAKE_WRIST_ROTATION_RANGE = 270.0;

    /** Minimum safe pickup distance (inches) */
    public static final double MIN_PICKUP_DISTANCE = 4.0;

    /** Optimal pickup distance (inches) */
    public static final double OPTIMAL_PICKUP_DISTANCE = 8.0;

}
