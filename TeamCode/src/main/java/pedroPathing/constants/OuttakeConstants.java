package pedroPathing.constants;

import com.acmerobotics.dashboard.config.Config;

/**
 * Constants for the outtake mechanism
 */
@Config
public class OuttakeConstants {
    // Prevent instantiation
    private OuttakeConstants() {}

    // PIDF coefficients for outtake slide control
    public static double SLIDE_P = 20;  // Starting value, needs tuning
    public static double SLIDE_I = 3;
    public static double SLIDE_D = 0; // Starting value, needs tuning
    public static double SLIDE_F = 0;  // Feed-forward for gravity compensation

    // Target position for the outtake slide (in encoder ticks)
    public static int SLIDE_TARGET_POSITION = 0;

    // Claw positions
    public static double CLAW_FULLY_OPEN = 0.575;
    public static double CLAW_OPEN = 0.68;
    public static double CLAW_CLOSED = 0.87;

    // Outtake elbow positions
    public static double OUTTAKE_ELBOW_BASE = 0.0;
    public static double OUTTAKE_ELBOW_GET_SPECIMEN = 0.98;
    public static double OUTTAKE_ELBOW_SCORE_SPECIMEN = 0.19;
    public static double OUTTAKE_ELBOW_BASKET = 0.7;
    public static double OUTTAKE_ELBOW_INIT = 0.17;
    public static double OUTTAKE_ELBOW_REST = 0.3;
    public static double OUTTAKE_ELBOW_TRANSITION = 0.27;

    // Outtake wrist positions
    public static double OUTTAKE_WRIST_GRAB = 0.54;
    public static double OUTTAKE_WRIST_TUCK = 0.9;
    public static double OUTTAKE_WRIST_BASKET = 0.65;
    public static double OUTTAKE_WRIST_GET_SPECIMEN = 0.35;
    public static double OUTTAKE_WRIST_SCORE_SPECIMEN = 0.62;
    public static double OUTTAKE_WRIST_INIT = 0.475;
    public static double OUTTAKE_WRIST_REST = 0.7;
    public static double OUTTAKE_WRIST_TRANSITION = 0.2;

    public static int OUTTAKE_CLAW_CLOSED_TIME = 100;

    public static int OUTTAKE_CLAW_FULL_CLOSED_TIME = 250;

    // Outtake slide positions
    public static int OUTTAKE_SLIDE_MIN = 0;
    public static int OUTTAKE_SLIDE_LIFT = 700;
    public static int OUTTAKE_SLIDE_SCORE = 1550;
    public static int OUTTAKE_SLIDE_BASKET = 2150;
    public static int OUTTAKE_SLIDE_MAX = 2150;  // Maximum safe extension

    public static int OUTTAKE_SLIDE_ELBOW_PREP = 1000;  // Maximum safe extension



    // Wait time (ms) before allowing 'A' button transition in MOVE_INTAKE_WRIST
    public static int MOVE_INTAKE_WRIST_A_BUTTON_WAIT_MS = 200;
}