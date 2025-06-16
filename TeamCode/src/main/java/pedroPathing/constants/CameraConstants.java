package pedroPathing.constants;

import com.acmerobotics.dashboard.config.Config;

/**
 * Camera Configuration Constants for Limelight 3A Vision System
 * 
 * This class contains all camera-related constants including physical mounting
 * parameters, field of view specifications, and sample detection settings.
 * 
 * These constants are used by the Python vision pipelines for accurate
 * distance calculations and robot-relative positioning.
 */
@Config
public class CameraConstants {
    
    // ========== LIMELIGHT 3A PHYSICAL MOUNTING ==========
    
    /**
     * Height of the Limelight 3A camera above the ground (inches)
     * Measure from ground to camera lens center
     */
    public static final double CAMERA_HEIGHT = 12.5;
    
    /**
     * Camera tilt angle downward from horizontal (degrees)
     * Positive values = tilted down, Negative values = tilted up
     */
    public static final double CAMERA_TILT_ANGLE = 25.0;
    
    /**
     * Camera offset from robot center - forward/backward direction (inches)
     * Positive = camera is forward of robot center (towards front of robot)
     * Negative = camera is behind robot center (towards back of robot)
     *
     * MEASURE: Distance from robot center to camera along robot's forward axis
     */
    public static double CAMERA_OFFSET_X = 9.84;

    /**
     * Camera offset from robot center - left/right direction (inches)
     * Positive = camera is to the LEFT of robot center (driver's left)
     * Negative = camera is to the RIGHT of robot center (driver's right)
     *
     * MEASURE: Distance from robot center to camera along robot's left-right axis
     */
    public static  double CAMERA_OFFSET_Y = 5.91;
    
    // ========== LIMELIGHT 3A SPECIFICATIONS ==========
    
    /**
     * Limelight 3A horizontal field of view (degrees)
     * This is a fixed specification of the Limelight 3A
     */
    public static final double CAMERA_FOV_HORIZONTAL = 59.6;
    
    /**
     * Limelight 3A vertical field of view (degrees)
     * This is a fixed specification of the Limelight 3A
     */
    public static final double CAMERA_FOV_VERTICAL = 45.7;
    
    /**
     * Camera resolution width (pixels)
     */
    public static final int CAMERA_WIDTH = 320;
    
    /**
     * Camera resolution height (pixels)
     */
    public static final int CAMERA_HEIGHT_PIXELS = 240;
    
    /**
     * Camera frame rate (fps)
     */
    public static final int CAMERA_FPS = 120;
    
    // ========== GAME PIECE SPECIFICATIONS ==========
    
    /**
     * Height of samples above the ground (inches)
     * This is the height of the sample center when sitting on the field
     */
    public static final double SAMPLE_HEIGHT = 1.0;
    
    /**
     * Approximate diameter of samples (inches)
     * Used for size validation and distance estimation
     */
    public static final double SAMPLE_DIAMETER = 3.5;
    
    // ========== DISTANCE CALCULATION LIMITS ==========
    
    /**
     * Minimum detectable distance (inches)
     * Samples closer than this may not be accurately detected
     */
    public static final double MIN_DETECTION_DISTANCE = 2.0;
    
    /**
     * Maximum detectable distance (inches)
     * Samples farther than this may not be accurately detected
     */
    public static final double MAX_DETECTION_DISTANCE = 150.0;
    
    /**
     * Default distance when calculation fails (inches)
     */
    public static final double DEFAULT_DISTANCE = 50.0;
    
    // ========== CAMERA SETTINGS ==========
    
    /**
     * Limelight 3A camera exposure setting (.01 ms units)
     * Current setting: 3300 = 33.00 ms
     */
    public static final int CAMERA_EXPOSURE = 3300;
    
    /**
     * Limelight 3A black level offset
     */
    public static final int CAMERA_BLACK_LEVEL_OFFSET = 4;
    
    /**
     * Limelight 3A sensor gain
     */
    public static final int CAMERA_SENSOR_GAIN = 19;
    
    /**
     * Limelight 3A red balance
     */
    public static final int CAMERA_RED_BALANCE = 1802;
    
    /**
     * Limelight 3A blue balance
     */
    public static final int CAMERA_BLUE_BALANCE = 1637;
    
    // ========== COORDINATE SYSTEM NOTES ==========
    /*
     * Robot Coordinate System (Standard FTC):
     *
     *     FRONT OF ROBOT
     *           ^
     *           | +X (forward)
     *           |
     *   +Y <----+----> -Y
     *  (left)   |    (right)
     *           |
     *           v -X (backward)
     *     BACK OF ROBOT
     *
     * - X-axis: Forward/Backward (+ = forward, - = backward)
     * - Y-axis: Left/Right (+ = left, - = right)
     * - Origin: Robot center
     * - Rotation: + = counterclockwise, - = clockwise
     *
     * Camera Coordinate System:
     * - Pixel (0,0) is top-left corner
     * - Pixel (320,240) is bottom-right corner
     * - Center pixel is (160,120)
     *
     * Sample Position Output:
     * - robotRelativeX: How far forward (+) or backward (-) the sample is
     * - robotRelativeY: How far left (+) or right (-) the sample is
     * - Both measured from robot center in inches
     */
    
    // ========== USAGE EXAMPLES ==========
    /*
     * Python Pipeline Usage:
     * 
     * # Import constants (if using Java constants in Python)
     * CAMERA_HEIGHT = 8.0  # Copy from CameraConstants.CAMERA_HEIGHT
     * 
     * # Calculate distance
     * distance = calculate_distance_to_sample(center_x, center_y, frame_width, frame_height)
     * 
     * # Calculate robot-relative position
     * robot_pos = calculate_robot_relative_position(center_x, center_y, frame_width, frame_height)
     * 
     * Java Usage:
     * 
     * // Access camera constants
     * double cameraHeight = CameraConstants.CAMERA_HEIGHT;
     * double tiltAngle = CameraConstants.CAMERA_TILT_ANGLE;
     * 
     * // Use in vision calculations
     * VisionResult result = visionSystem.processFrame();
     * double distance = result.getDistance();
     */
}
