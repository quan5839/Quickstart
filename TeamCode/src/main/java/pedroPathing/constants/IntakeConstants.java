package pedroPathing.constants;

import com.acmerobotics.dashboard.config.Config;

@Config
public class IntakeConstants {
    private IntakeConstants() {}
    public static double CLAW_OPEN = 0.58;
    public static double CLAW_CLOSED = 0.4;

    public static double TURRET_MIDDLE = 0.5;
    public static double TURRET_RIGHT = 0.055;
    public static double TURRET_LEFT = 0.99;
    public static double TURRET_MIN_OFFSET = 0;
    public static double TURRET_MAX_OFFSET = 0.985;
    public static double TURRET_REMOVE = 0.1;


    public static double SHOULDER_GRAB = 0.0;
    public static double SHOULDER_HOLD = 0.6;
    public static double SHOULDER_INIT = 0.6;
    public static double SHOULDER_OUTTAKE_TRANSITION = 0.22;
    public static double SHOULDER_OUTTAKE_TRANSITION_PREP = 0.12;
    public static double SHOULDER_PREP = 0.025;
    public static double SHOULDER_REMOVE = 0.1;
    public static double SHOULDER_RELEASE = 0.1;
    public static double SHOULDER_REST = 0.5;

    public static double ELBOW_GRAB = 0.38;
    public static double ELBOW_HOLD = 0.2;
    public static double ELBOW_INIT = 0.1;
    public static double ELBOW_OUTTAKE_TRANSITION = 1;
    public static double ELBOW_PREP = 0.365;
    public static double ELBOW_RELEASE = 0.6;
    public static double ELBOW_REST = 0.15;
    public static double ELBOW_REMOVE = 0.6;

    public static double SLIDE_MIN = 0.88;
    public static double SLIDE_MAX = 0;
    public static double SLIDE_MAX_OVERRIDE = 0;
    public static double SLIDE_MAX_EXTENSION = 0.26;
    public static double SLIDE_CONTRACT = 0.55;
    public static double SLIDE_RELEASE = 0.32;
    public static double SLIDE_HOLD = 1;

    public static double WRIST_MIDDLE = 0.5;

    public static int INTAKE_CLAW_RELEASE = 300;
    public static int INTAKE_SHOULDER_OUTTAKE_TRANSITION_PREP_TIME = 800;
    public static int INTAKE_SHOULDER_GRAB_CLOSE_TIME = 250;
    public static int CLAW_CLOSED_TIME = 175;
    public static int SAMPLE_REMOVE_TIME = 200;
    public static int SLIDE_RELEASE_TIME = 150;
    public static int SLIDE_HOLD_POSITION_END_DELAY = 0; // Delay after slide reaches hold position
    public static int INTAKE_LIMIT_SWITCH_DELAY = 600; // Delay after slide reaches hold position
    // ===== AUTOMATIC INTAKE CONSTANTS =====

    /** Maximum reach distance for automatic intake (inches) */
    public static double AUTO_INTAKE_MAX_REACH = 15.75; // 40cm converted to inches

    /** Minimum distance for safe automatic pickup (inches) */
    public static double AUTO_INTAKE_MIN_DISTANCE = 3.0;

    /** Maximum angle for turret automatic positioning (degrees) */
    public static double AUTO_INTAKE_MAX_ANGLE = 135.0;

    /** Timing constants for automatic intake sequence (milliseconds) */
    public static int AUTO_TURRET_MOVE_TIME = 500;
    public static int AUTO_SLIDE_EXTEND_TIME = 800;
    public static int AUTO_ARM_POSITION_TIME = 600;
    public static int AUTO_WRIST_ADJUST_TIME = 300;

    /** Safety margins for automatic positioning */
    public static double AUTO_POSITION_TOLERANCE = 2.0; // inches
    public static double AUTO_ANGLE_TOLERANCE = 10.0;   // degrees

    // ===== ARM KINEMATICS CONSTANTS =====

    /** Length from shoulder joint to elbow joint (mm) */
    public static double SHOULDER_TO_ELBOW_LENGTH_MM = 172.353;

    /** Length from shoulder joint to elbow joint (inches) */
    public static double SHOULDER_TO_ELBOW_LENGTH_INCHES = SHOULDER_TO_ELBOW_LENGTH_MM / 25.4;

    /** Estimated length from elbow to claw tip (inches) - MEASURE THIS */
    public static double ELBOW_TO_CLAW_LENGTH_INCHES = 4.0; // Placeholder - needs measurement

    /** Total arm reach from shoulder pivot to claw tip (inches) */
    public static double TOTAL_ARM_REACH_INCHES = SHOULDER_TO_ELBOW_LENGTH_INCHES + ELBOW_TO_CLAW_LENGTH_INCHES;

    /** Height of shoulder joint above ground when robot is level (inches) - MEASURE THIS */
    public static double SHOULDER_HEIGHT_INCHES = 8.0; // Placeholder - needs measurement





}
