package pedroPathing.constants;

import com.acmerobotics.dashboard.config.Config;

/**
 * Constants for the outtake mechanism, organized alphabetically within sections
 */
@Config
public class OuttakeConstants {
    // Prevent instantiation
    private OuttakeConstants() {}

    // ========== CLAW POSITIONS ==========

    /** Claw closed position */
    public static double CLAW_CLOSED = 0.87;

    /** Claw fully open position */
    public static double CLAW_FULLY_OPEN = 0.575;

    /** Claw open position */
    public static double CLAW_OPEN = 0.68;

    // ========== ELBOW POSITIONS ==========

    /** Elbow base position */
    public static double ELBOW_BASE = 0.0;

    /** Elbow basket position */
    public static double ELBOW_BASKET = 0.55;

    /** Elbow get specimen position */
    public static double ELBOW_GET_SPECIMEN = 0.98;

    /** Elbow initialization position */
    public static double ELBOW_INIT = 0.17;

    /** Elbow rest position */
    public static double ELBOW_REST = 0.3;

    /** Elbow score specimen position */
    public static double ELBOW_SCORE_SPECIMEN = 0.17;

    /** Elbow transition position */
    public static double ELBOW_TRANSITION = 0.27;


    public static double ELBOW_PARK = 0.64;



    // ========== SLIDE CONTROL ==========

    /** PIDF D coefficient for outtake slide control */
    public static double SLIDE_D = 0;

    /** PIDF F coefficient - feed-forward for gravity compensation */
    public static double SLIDE_F = 0;

    /** PIDF I coefficient for outtake slide control */
    public static double SLIDE_I = 3;

    /** PIDF P coefficient for outtake slide control */
    public static double SLIDE_P = 20;

    /** Target position for the outtake slide (in encoder ticks) */
    public static int SLIDE_TARGET_POSITION = 0;

    // ========== SLIDE POSITIONS ==========

    /** Slide basket position */
    public static int SLIDE_BASKET = 2000;

    /** Slide elbow prep position */
    public static int SLIDE_ELBOW_PREP = 1000;
    /** Slide lift position */
    public static int SLIDE_LIFT = 700;

    /** Maximum safe extension */
    public static int SLIDE_MAX = 2150;

    /** Slide minimum position */
    public static int SLIDE_MIN = 0;

    /** Slide score position */
    public static int SLIDE_SCORE = 1550;

    // ========== TIMING CONSTANTS ==========

    /** Claw closed time (ms) */
    public static int CLAW_CLOSED_TIME = 100;

    /** Claw full closed time (ms) */
    public static int CLAW_FULL_CLOSED_TIME = 250;

    /** Wait time before allowing 'A' button transition in MOVE_INTAKE_WRIST (ms) */
    public static int MOVE_INTAKE_WRIST_A_BUTTON_WAIT_MS = 200;

    // ========== WRIST POSITIONS ==========

    /** Wrist basket position */
    public static double WRIST_BASKET = 0.65;

    /** Wrist get specimen position */
    public static double WRIST_GET_SPECIMEN = 0.35;

    /** Wrist grab position */
    public static double WRIST_GRAB = 0.54;

    /** Wrist initialization position */
    public static double WRIST_INIT = 0.475;

    /** Wrist rest position */
    public static double WRIST_REST = 0.7;

    /** Wrist score specimen position */
    public static double WRIST_SCORE_SPECIMEN = 0.55;

    /** Wrist transition position */
    public static double WRIST_TRANSITION = 0.2;

    /** Wrist tuck position */
    public static double WRIST_TUCK = 0.9;
}