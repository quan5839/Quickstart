package pedroPathing.constants;

import com.acmerobotics.dashboard.config.Config;

@Config
public class SpecimenAutoConstant {
    private SpecimenAutoConstant() {
    }

    // ===== TIMING CONSTANTS =====
    public static long OUTTAKE_CLAW_RELEASE = 1500;

    public static long INTAKE_SLIDE_WAIT_TIME = 2350;


    // Park Poses position and control point
    public static double PARK_X = 20;
    public static double PARK_Y = 40;
    public static double PARK_HEADING_DEG = 230;

    public static double PARK_CONTROL_X = 26;
    public static double PARK_CONTROL_Y = 70;

    // ===== SPECIMEN WAYPOINT CONSTANTS FROM PATH PICTURES =====

    public static double RETRIEVE_HEADING_DEG = 235;
    public static double GRAB_SCORE_HEADING_DEG = 0;
    public static double SCORE_X = 38;
    public static double SCORE_CONTROL_X = 13;
    public static double SCORE_CONTROL_Y = 70;
    public static double SCORE_PRELOAD_Y = 72;
    public static double RETRIEVE_SAMPLE_TURRET_POS = 0.8;
    public static double PUSH_RETRIEVE_PICKUP_X = 20;



    public static double PREP_GRAB_SPECIMEN_X = 17;
    public static double PREP_AND_GRAB_SPECIMEN_Y = 24;
    public static double PREP_GRAB_SPECIMEN_CONTROL_X_1 = 20;
    public static double PREP_GRAB_SPECIMEN_CONTROL_Y_1 = 70;
    public static double PREP_GRAB_SPECIMEN_CONTROL_X_2 = 50;
    public static double PREP_GRAB_SPECIMEN_CONTROL_Y_2 = 24;

    public static double GRAB_SPECIMEN_X = 13;


    public static double START_X = 7;
    public static double START_Y = 72;
    public static double START_HEADING_DEG = 0;

    public static double GRAB_PICKUP1_X = 28;
    public static double GRAB_PICKUP1_Y = 46;

    public static double PUSH_PICKUP1_PREP_X = 48;
    public static double PUSH_PICKUP1_PREP_Y = 36;
    public static double PUSH_PICKUP1_PREP_CONTROL_X = 33;
    public static double PUSH_PICKUP1_PREP_CONTROL_Y = 36;
    public static double PUSH_PICKUP1_X = 48;
    public static double PUSH_PICKUP1_Y = 28;
    public static double PUSH_PICKUP1_CONTROL_X = 70;
    public static double PUSH_PICKUP1_CONTROL_Y = 30;
    public static double PUSH_RETRIEVE_PICKUP1_Y = 28;


    public static double PICKUP1_HEADING_DEG = 310;
    public static double SAMPLE1_TURRET_POS = 0.7;
    public static double SAMPLE1_WRIST_POS = 0.4;

    public static double SCORE1_Y = 71;

    public static double GRAB_PICKUP1_CONTROL_X = 26;
    public static double GRAB_PICKUP1_CONTROL_Y = 70;

    public static double GRAB_PICKUP2_X = 28;
    public static double GRAB_PICKUP2_Y = 36;

    public static double PUSH_PICKUP2_X = 62;
    public static double PUSH_PICKUP2_Y = 18;
    public static double PUSH_PICKUP2_CONTROL_X = 62;
    public static double PUSH_PICKUP2_CONTROL_Y = 28;
    public static double PUSH_RETRIEVE_PICKUP2_Y = 18;


    public static double SCORE2_Y = 70;
    public static double SAMPLE2_TURRET_POS = 0.65;
    public static double SAMPLE2_WRIST_POS = 0.8;
    public static double PICKUP2_HEADING_DEG = 310;


    public static double GRAB_PICKUP3_X = 28;
    public static double GRAB_PICKUP3_Y = 26;

    public static double PUSH_PICKUP3_X = 62;
    public static double PUSH_PICKUP3_Y = 8;
    public static double PUSH_PICKUP3_CONTROL_X = 62;
    public static double PUSH_PICKUP3_CONTROL_Y = 18;
    public static double PUSH_RETRIEVE_PICKUP3_Y = 8;


    public static double SCORE3_Y = 69;
    public static double SAMPLE3_TURRET_POS = 0.8;
    public static double SAMPLE3_WRIST_POS = 0.3;
    public static double PICKUP3_HEADING_DEG = 310;

    public static double SCORE4_Y = 68;



}
