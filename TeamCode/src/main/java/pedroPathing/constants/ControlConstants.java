package pedroPathing.constants;

import com.acmerobotics.dashboard.config.Config;

/**
 * Constants for robot control settings
 */
@Config
public class ControlConstants {
    // Prevent instantiation
    private ControlConstants() {
    }


    // Servo velocity control
    public static double MAX_SERVO_ACCELERATION = 3.0;
    public static double SLIDE_MAX_VELOCITY = 0.4;
    public static double WRIST_MAX_VELOCITY = 1;

    // Minimum drive power to break static friction
    public static double MIN_DRIVE_POWER = 0;

    public static double PRECISION_MODE_SCALE = 0.25; // Scale factor for precision mode

    // Auto-orientation constants
    public static double AUTO_ORIENTATION_P_GAIN = 1.5; // Match the old Kp value
    public static double AUTO_ORIENTATION_D_GAIN = 0.08; // D gain for heading correction
    public static double AUTO_ORIENTATION_DEADBAND_DEG = 0.8; // Deadband in degrees
    public static double AUTO_ORIENTATION_OVERRIDE_THRESHOLD = 0.1; // Small threshold to detect any turn input

    // Active heading hold/correction constants
    public static double ACTIVE_HEADING_P_GAIN = 1.0;
    public static double ACTIVE_HEADING_D_GAIN = 0.08;
    public static double ACTIVE_HEADING_TURN_DEADBAND = 0.1;
    public static double ACTIVE_HEADING_MIN_TURN_POWER = 0.05;
    public static double ACTIVE_HEADING_HOLD_DEADBAND_DEG = 1.0;
    public static boolean ENABLE_ACTIVE_HEADING_CORRECTION = true;

    // Performance optimization constants
    public static long TELEMETRY_UPDATE_INTERVAL_MS = 100; // Reduced telemetry frequency for better performance
    public static long COLOR_READ_INTERVAL_MS = 50; // Base color sensor read interval to reduce I2C calls

    // Adaptive color sensor intervals for optimal performance
    public static long COLOR_READ_FAST_INTERVAL_MS = 25; // Fast reads when sample detected (responsiveness)
    public static long COLOR_READ_SLOW_INTERVAL_MS = 250; // Slow reads when no sample (efficiency)

    // Color detection constants - unified for both intake and outtake systems
    public static final double COLOR_DETECTION_MAX_DISTANCE_CM = 2.5; // Max distance to detect color
    public static final double SAMPLE_DETECTION_DISTANCE_CM = 1.0; // Distance threshold for sample presence
    public static final double SPECIMEN_DETECTION_DISTANCE_CM = 3.0; // Distance threshold for specimen presence in outtake

    // Unified RGB ranges for consistent color detection across all systems
    // Blue sample detection ranges
    public static double BLUE_R_MIN = 30;
    public static double BLUE_R_MAX = 280;
    public static double BLUE_G_MIN = 175;
    public static double BLUE_G_MAX = 500;
    public static double BLUE_B_MIN = 425;
    public static double BLUE_B_MAX = 850;

    // Yellow sample detection ranges
    public static double YELLOW_R_MIN = 600;
    public static double YELLOW_R_MAX = 1000;
    public static double YELLOW_G_MIN = 900;
    public static double YELLOW_G_MAX = 1200;
    public static double YELLOW_B_MIN = 200;
    public static double YELLOW_B_MAX = 600;

    // Red sample detection ranges
    public static double RED_R_MIN = 400;
    public static double RED_R_MAX = 550;
    public static double RED_G_MIN = 250;
    public static double RED_G_MAX = 520;
    public static double RED_B_MIN = 100;
    public static double RED_B_MAX = 500;

    // Sample feedback constants
    public static final long SAMPLE_FEEDBACK_DURATION_MS = 3000; // 3 seconds of feedback
    public static final long FAST_BLINK_INTERVAL_MS = 150; // Fast blink for alliance samples
    public static final long SLOW_BLINK_INTERVAL_MS = 300; // Slow blink for opponent samples
    public static final long ULTRA_FAST_BLINK_INTERVAL_MS = 75; // Ultra fast for neutral samples

    // Loop performance tracking
    public static final int PERCENTILE_BUFFER_SIZE = 100; // Track last 100 samples for percentile calculation

    // If true, enables debug mode where the state machine steps only with gamepad2.x and also always reapplies servo state every loop
    public static boolean STATE_MACHINE_DEBUG_MODE = false;

    // Legacy intake color constants removed - now using unified color constants above

    // State machine timing constants (extracted from magic numbers)
    public static final long COLOR_CHECK_DURATION_MS = 1000; // Duration for color checking in intake states
    public static final long OUTTAKE_RESET_DELAY_MS = 1000; // Default delay for outtake reset
    public static final long OUTTAKE_RESET_DELAY_LONG_MS = 2500; // Longer delay for complex outtake reset
    public static final int SPECIMEN_RELEASE_DELAY_MS = 500; // Delay for specimen release action
    public static final int MODE_SWITCH_RUMBLE_DURATION_MS = 500; // Gamepad rumble duration for mode switch
    public static final int SAMPLE_AUTO_DUMP_DELAY_MS = 500; // Auto-dump delay for sample outtake

    // If true, only alliance color is auto-accepted in SPECIMEN_INTAKE_CHECK_SAMPLE, yellow is ignored
    public static boolean ENABLE_ALLIANCE_ONLY_SPECIMEN = true;

    // If true, state machine auto-advances in intake based on color sensor; if false, only operator input advances state.
    public static boolean ENABLE_AUTO_COLOR_ADVANCE = true;

    public static boolean ENABLE_BASKET_ALLIANCE_SAMPLE_INTAKE = true;

    public static boolean ENABLE_AUTO_REMOVE_OPPONENT_SAMPLE = false;

    // If true, outtake color sensor distance detection auto-advances SPECIMEN_OUTTAKE_CLAW_CLOSE state
    public static boolean ENABLE_AUTO_SPECIMEN_OUTTAKE_ADVANCE = true;

    // Drive system performance constants
    public static final double STRAFE_SCALING_FACTOR = 1.1; // Extracted magic number
    public static final double NOMINAL_VOLTAGE = 12.0; // Standard FTC battery voltage
    public static final double MIN_VOLTAGE_COMPENSATION = 0.8; // Minimum voltage compensation factor

    // Performance optimization constants
    public static final int SERVO_MAP_INITIAL_CAPACITY = 16; // Optimize HashMap sizing
    public static final int TELEMETRY_BUILDER_CAPACITY = 512; // Pre-size StringBuilder

    // Servo position caching constants
    public static final double SERVO_POSITION_TOLERANCE = 0.001; // Minimum change to trigger servo write

    // State machine performance constants
    public static final int STATE_TRANSITION_ARRAY_SIZE = 100; // Pre-allocate transition arrays

    // Memory optimization constants
    public static final int INITIAL_SET_CAPACITY = 32; // For HashSet pre-sizing
}
