package pedroPathing.constants;

import com.acmerobotics.dashboard.config.Config;

/**
 * Constants for the intake mechanism, organized alphabetically within sections
 */
@Config
public class IntakeConstants {
    private IntakeConstants() {}

    // ========== ARM KINEMATICS ==========


    /** Length from shoulder joint to elbow joint (inches) */
    public static double SHOULDER_TO_ELBOW_LENGTH_INCHES = 6.79;



    /** Total arm reach from shoulder pivot to claw tip (inches) */
    public static double TOTAL_ARM_REACH_INCHES = SHOULDER_TO_ELBOW_LENGTH_INCHES ;

    // ========== AUTOMATIC INTAKE ==========
    
    /** Safety margins for automatic positioning - angle tolerance (degrees) */
    public static double AUTO_ANGLE_TOLERANCE = 10.0;
    
    /** Timing constants for automatic intake sequence - arm position time (ms) */
    public static int AUTO_ARM_POSITION_TIME = 600;
    
    /** Maximum reach distance for automatic intake (inches) */
    public static double AUTO_INTAKE_MAX_SLIDE_REACH = 15.75; // 40cm converted to inches
    
    /** Maximum angle for turret automatic positioning - LEFT side (degrees) */
    public static double AUTO_INTAKE_TURRET_LEFT_MAX_ANGLE = 100.0;

    /** Maximum angle for turret automatic positioning - RIGHT side (degrees) */
    public static double AUTO_INTAKE_TURRET_RIGHT_MAX_ANGLE = 115.0;

    /** Legacy maximum angle for backward compatibility (uses the smaller of left/right) */
    public static double AUTO_INTAKE_TURRET_MAX_ANGLE = Math.min(AUTO_INTAKE_TURRET_LEFT_MAX_ANGLE, AUTO_INTAKE_TURRET_RIGHT_MAX_ANGLE);
    
    /** Minimum distance for safe automatic pickup (inches) */
    public static double AUTO_INTAKE_MIN_DISTANCE = 3.0;
    
    /** Safety margins for automatic positioning - position tolerance (inches) */
    public static double AUTO_POSITION_TOLERANCE = 2.0;
    
    /** Timing constants for automatic intake sequence - slide extend time (ms) */
    public static int AUTO_SLIDE_EXTEND_TIME = 800;
    
    /** Timing constants for automatic intake sequence - turret move time (ms) */
    public static int AUTO_TURRET_MOVE_TIME = 500;
    
    /** Timing constants for automatic intake sequence - wrist adjust time (ms) */
    public static int AUTO_WRIST_ADJUST_TIME = 300;

    // ========== STADIUM AREA CALCULATIONS ==========

    /** Slide maximum extension length (inches) - 40cm converted */
    public static double SLIDE_LENGTH_INCHES = 15.75;

    /** Shoulder joint rotation radius (inches) - 17cm converted */
    public static double SHOULDER_RADIUS_INCHES = 6.69;

    /** Enable stadium area boundary checking */
    public static boolean ENABLE_STADIUM_AREA_CHECK = true;

    /** Stadium area safety margin (inches) */
    public static double STADIUM_SAFETY_MARGIN = 2.0;

    // ========== CLAW POSITIONS ==========
    
    /** Claw closed position */
    public static double CLAW_CLOSED = 0.4;
    
    /** Claw open position */
    public static double CLAW_OPEN = 0.58;

    // ========== ELBOW POSITIONS ==========
    
    /** Elbow grab position */
    public static double ELBOW_GRAB = 0.38;
    
    /** Elbow hold position */
    public static double ELBOW_HOLD = 0.2;
    
    /** Elbow initialization position */
    public static double ELBOW_INIT = 0.1;
    
    /** Elbow outtake transition position */
    public static double ELBOW_OUTTAKE_TRANSITION = 1;
    
    /** Elbow prep position */
    public static double ELBOW_PREP = 0.365;
    
    /** Elbow release position */
    public static double ELBOW_RELEASE = 0.6;
    
    /** Elbow remove position */
    public static double ELBOW_REMOVE = 0.6;
    
    /** Elbow rest position */
    public static double ELBOW_REST = 0.15;

    // ========== SHOULDER POSITIONS ==========
    
    /** Shoulder grab position */
    public static double SHOULDER_GRAB = 0.02;
    
    /** Shoulder hold position */
    public static double SHOULDER_HOLD = 0.66;
    
    /** Shoulder initialization position */
    public static double SHOULDER_INIT = 0.66;
    
    /** Shoulder outtake transition position */
    public static double SHOULDER_OUTTAKE_TRANSITION = 0.28;
    
    /** Shoulder outtake transition prep position */
    public static double SHOULDER_OUTTAKE_TRANSITION_PREP = 0.22;
    
    /** Shoulder prep position */
    public static double SHOULDER_PREP = 0.09;
    
    /** Shoulder release position */
    public static double SHOULDER_RELEASE = 0.16;
    
    /** Shoulder remove position */
    public static double SHOULDER_REMOVE = 0.16;
    
    /** Shoulder rest position */
    public static double SHOULDER_REST = 0.56;

    // ========== SLIDE POSITIONS ==========
    
    /** Slide contract position */
    public static double SLIDE_CONTRACT = 0.55;
    
    /** Slide hold position */
    public static double SLIDE_HOLD = 1;
    
    /** Slide maximum position */
    public static double SLIDE_MAX = 0.015;
    
    /** Slide maximum extension position */
    public static double SLIDE_MAX_EXTENSION = 0.26;
    
    /** Slide maximum override position */
    public static double SLIDE_MAX_OVERRIDE = 0;
    
    /** Slide minimum position */
    public static double SLIDE_MIN = 0.29;
    
    /** Slide release position */
    public static double SLIDE_RELEASE = 0;

    // ========== TIMING CONSTANTS ==========
    
    /** Claw closed time (ms) */
    public static int CLAW_CLOSED_TIME = 80;
    
    /** Intake claw release time (ms) */
    public static int INTAKE_CLAW_RELEASE = 300;
    
    /** Intake limit switch delay (ms) */
    public static int INTAKE_LIMIT_SWITCH_DELAY = 500;
    
    /** Intake shoulder grab close time (ms) */
    public static int INTAKE_SHOULDER_GRAB_CLOSE_TIME = 250;
    
    /** Intake shoulder outtake transition prep time (ms) */
    public static int INTAKE_SHOULDER_OUTTAKE_TRANSITION_PREP_TIME = 550;
    
    /** Sample remove time (ms) */
    public static int SAMPLE_REMOVE_TIME = 200;
    
    /** Delay after slide reaches hold position (ms) */
    public static int SLIDE_HOLD_POSITION_END_DELAY = 0;
    
    /** Slide release time (ms) */
    public static int SLIDE_RELEASE_TIME = 200;

    /** Auto-return delay from hold to min position (ms) */
    public static int SLIDE_AUTO_RETURN_DELAY = 800;

    public static int INTAKE_ELBOWS_PREP = 500;


    // ========== TURRET POSITIONS ==========
    
    /** Turret left position */
    public static double TURRET_LEFT = 0.91;
    
    /** Turret maximum offset */
    public static double TURRET_MAX_OFFSET = 0.985;
    
    /** Turret middle position */
    public static double TURRET_MIDDLE = 0.5;
    
    /** Turret minimum offset */
    public static double TURRET_MIN_OFFSET = 0;
    
    /** Turret remove position */
    public static double TURRET_REMOVE = 0.1;
    
    /** Turret right position */
    public static double TURRET_RIGHT = 0.055;

    // ========== WRIST POSITIONS ==========
    
    /** Wrist middle position */
    public static double WRIST_MIDDLE = 0.5;
}
