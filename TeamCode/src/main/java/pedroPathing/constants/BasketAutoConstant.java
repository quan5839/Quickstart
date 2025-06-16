package pedroPathing.constants;

import com.acmerobotics.dashboard.config.Config;

@Config
public class BasketAutoConstant {
    private BasketAutoConstant() {
    }

    // ===== TIMING CONSTANTS =====
    public static long OUTTAKE_CLAW_RELEASE = 1500;

    public static long INTAKE_SLIDE_WAIT_TIME = 2350;






    // ===== POSEANTS =====

    // Start Poseot starting position
    public static double START_X = 7;
    public static double START_Y = 110;
    public static double START_HEADING_DEG = 270;

    // Score Poseition for scoring in basket
    public static double SCORE_X = 13.5;
    public static double SCORE_Y = 127;
    public static double SCORE_HEADING_DEG = 315;

    // Pickup Posample pickup positions
    public static double PICKUP1_X = 11.5;
    public static double PICKUP1_Y = 118.55;
    public static double PICKUP1_HEADING_DEG = 356;
    public static double SAMPLE1_TURRET_POS = 0.7;
    public static double SAMPLE1_WRIST_POS = 0.4;


    public static double PICKUP2_X = 11.2;
    public static double PICKUP2_Y = 128.6;
    public static double PICKUP2_HEADING_DEG = 354;
    public static double SAMPLE2_TURRET_POS = 0.65;
    public static double SAMPLE2_WRIST_POS = 0.8;



    public static double PICKUP3_X = 14;
    public static double PICKUP3_Y = 132.5;
    public static double PICKUP3_HEADING_DEG = 0;
    public static double SAMPLE3_TURRET_POS = 0.8;
    public static double SAMPLE3_WRIST_POS = 0.3;



    // Park Poses position and control point
    public static double PARK_X = 60;
    public static double PARK_Y = 98;
    public static double PARK_HEADING_DEG = 90;

    public static double PARK_CONTROL_X = 60;
    public static double PARK_CONTROL_Y = 98;
    public static double PARK_CONTROL_HEADING_DEG = 90;

}
