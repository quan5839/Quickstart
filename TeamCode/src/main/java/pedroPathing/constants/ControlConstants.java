package pedroPathing.constants;

import com.acmerobotics.dashboard.config.Config;

/**
 * Constants for robot control settings, organized alphabetically within sections
 */
@Config
public class ControlConstants {
    // Prevent instantiation
    private ControlConstants() {
    }

    // ========== ACTIVE HEADING CONTROL ==========

    /** D gain for active heading correction */
    public static double ACTIVE_HEADING_D_GAIN = 0.08;

    /** Deadband for heading hold in degrees */
    public static double ACTIVE_HEADING_HOLD_DEADBAND_DEG = 1.0;

    /** Minimum turn power for active heading correction */
    public static double ACTIVE_HEADING_MIN_TURN_POWER = 0.05;

    /** P gain for active heading correction */
    public static double ACTIVE_HEADING_P_GAIN = 1.0;

    /** Turn deadband for active heading correction */
    public static double ACTIVE_HEADING_TURN_DEADBAND = 0.1;

    /** Enable/disable active heading correction */
    public static boolean ENABLE_ACTIVE_HEADING_CORRECTION = true;

    // ========== AUTO-ORIENTATION CONTROL ==========

    /** Deadband in degrees for auto-orientation */
    public static double AUTO_ORIENTATION_DEADBAND_DEG = 0.8;

    /** D gain for auto-orientation heading correction */
    public static double AUTO_ORIENTATION_D_GAIN = 0.08;

    /** Small threshold to detect any turn input */
    public static double AUTO_ORIENTATION_OVERRIDE_THRESHOLD = 0.1;

    /** P gain for auto-orientation (match the old Kp value) */
    public static double AUTO_ORIENTATION_P_GAIN = 1.5;

    // ========== COLOR DETECTION ==========

    /** Blue sample B channel maximum value */
    public static double BLUE_B_MAX = 850;

    /** Blue sample B channel minimum value */
    public static double BLUE_B_MIN = 425;

    /** Blue sample G channel maximum value */
    public static double BLUE_G_MAX = 500;

    /** Blue sample G channel minimum value */
    public static double BLUE_G_MIN = 175;

    /** Blue sample R channel maximum value */
    public static double BLUE_R_MAX = 280;

    /** Blue sample R channel minimum value */
    public static double BLUE_R_MIN = 30;

    /** Maximum distance to detect color (cm) */
    public static final double COLOR_DETECTION_MAX_DISTANCE_CM = 2.5;

    /** Base color sensor read interval to reduce I2C calls (ms) */
    public static long COLOR_READ_INTERVAL_MS = 50;

    /** Fast reads when sample detected for responsiveness (ms) */
    public static long COLOR_READ_FAST_INTERVAL_MS = 25;

    /** Slow reads when no sample for efficiency (ms) */
    public static long COLOR_READ_SLOW_INTERVAL_MS = 250;

    /** Red sample B channel maximum value */
    public static double RED_B_MAX = 500;

    /** Red sample B channel minimum value */
    public static double RED_B_MIN = 100;

    /** Red sample G channel maximum value */
    public static double RED_G_MAX = 520;

    /** Red sample G channel minimum value */
    public static double RED_G_MIN = 250;

    /** Red sample R channel maximum value */
    public static double RED_R_MAX = 550;

    /** Red sample R channel minimum value */
    public static double RED_R_MIN = 400;

    /** Distance threshold for sample presence (cm) */
    public static final double SAMPLE_DETECTION_DISTANCE_CM = 1.0;

    /** Distance threshold for specimen presence in outtake (cm) */
    public static final double SPECIMEN_DETECTION_DISTANCE_CM = 3.0;

    /** Yellow sample B channel maximum value */
    public static double YELLOW_B_MAX = 600;

    /** Yellow sample B channel minimum value */
    public static double YELLOW_B_MIN = 200;

    /** Yellow sample G channel maximum value */
    public static double YELLOW_G_MAX = 1200;

    /** Yellow sample G channel minimum value */
    public static double YELLOW_G_MIN = 900;

    /** Yellow sample R channel maximum value */
    public static double YELLOW_R_MAX = 1000;

    /** Yellow sample R channel minimum value */
    public static double YELLOW_R_MIN = 600;

    // ========== DRIVE SYSTEM ==========

    /** Minimum drive power to break static friction */
    public static double MIN_DRIVE_POWER = 0;

    /** Minimum voltage compensation factor */
    public static final double MIN_VOLTAGE_COMPENSATION = 0.8;

    /** Standard FTC battery voltage */
    public static final double NOMINAL_VOLTAGE = 12.0;

    /** Scale factor for precision mode */
    public static double PRECISION_MODE_SCALE = 0.25;

    /** Strafe scaling factor (extracted magic number) */
    public static final double STRAFE_SCALING_FACTOR = 1.1;

    // ========== FEATURE TOGGLES ==========

    /** Enable/disable alliance-only specimen intake (ignores yellow) */
    public static boolean ENABLE_ALLIANCE_ONLY_SPECIMEN = true;

    /** Enable/disable auto color advance in state machine */
    public static boolean ENABLE_AUTO_COLOR_ADVANCE = true;

    /** Enable/disable automatic opponent sample removal */
    public static boolean ENABLE_AUTO_REMOVE_OPPONENT_SAMPLE = false;

    /** Enable/disable auto specimen outtake advance */
    public static boolean ENABLE_AUTO_SPECIMEN_OUTTAKE_ADVANCE = true;

    /** Enable/disable basket alliance sample intake */
    public static boolean ENABLE_BASKET_ALLIANCE_SAMPLE_INTAKE = true;

    // ========== FEEDBACK & VISUAL ==========

    /** Fast blink interval for alliance samples (ms) */
    public static final long FAST_BLINK_INTERVAL_MS = 150;

    /** Sample feedback duration (ms) - 3 seconds of feedback */
    public static final long SAMPLE_FEEDBACK_DURATION_MS = 3000;

    /** Slow blink interval for opponent samples (ms) */
    public static final long SLOW_BLINK_INTERVAL_MS = 300;

    /** Ultra fast blink for neutral samples (ms) */
    public static final long ULTRA_FAST_BLINK_INTERVAL_MS = 75;

    // ========== PERFORMANCE OPTIMIZATION ==========

    /** For HashSet pre-sizing */
    public static final int INITIAL_SET_CAPACITY = 32;

    /** Track last 100 samples for percentile calculation */
    public static final int PERCENTILE_BUFFER_SIZE = 100;

    /** Optimize HashMap sizing for servos */
    public static final int SERVO_MAP_INITIAL_CAPACITY = 16;

    /** Pre-allocate transition arrays for state machine */
    public static final int STATE_TRANSITION_ARRAY_SIZE = 100;

    /** Pre-size StringBuilder for telemetry */
    public static final int TELEMETRY_BUILDER_CAPACITY = 512;

    /** Reduced telemetry frequency for better performance (ms) */
    public static long TELEMETRY_UPDATE_INTERVAL_MS = 100;

    // ========== SERVO CONTROL ==========

    /** Maximum servo acceleration */
    public static double MAX_SERVO_ACCELERATION = 3.0;

    /** Minimum change to trigger servo write */
    public static final double SERVO_POSITION_TOLERANCE = 0.001;

    /** Maximum slide velocity */
    public static double SLIDE_MAX_VELOCITY = 0.4;

    /** Maximum wrist velocity */
    public static double WRIST_MAX_VELOCITY = 1;

    // ========== STATE MACHINE ==========

    /** Duration for color checking in intake states (ms) */
    public static final long COLOR_CHECK_DURATION_MS = 1000;

    /** Gamepad rumble duration for mode switch (ms) */
    public static final int MODE_SWITCH_RUMBLE_DURATION_MS = 500;

    /** Default delay for outtake reset (ms) */
    public static final long OUTTAKE_RESET_DELAY_MS = 1000;

    /** Longer delay for complex outtake reset (ms) */
    public static final long OUTTAKE_RESET_DELAY_LONG_MS = 2500;

    /** Auto-dump delay for sample outtake (ms) */
    public static final int SAMPLE_AUTO_DUMP_DELAY_MS = 500;

    /** Delay for specimen release action (ms) */
    public static final int SPECIMEN_RELEASE_DELAY_MS = 400;

    /** Enable debug mode for state machine stepping */
    public static boolean STATE_MACHINE_DEBUG_MODE = false;
}
