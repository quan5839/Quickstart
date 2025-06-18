package pedroPathing.constants;

import com.acmerobotics.dashboard.config.Config;

/**
 * Autonomous constants specific to specimen scoring mode
 * Organized alphabetically within sections
 */
@Config
public class SpecimenAutoConstants {
    private SpecimenAutoConstants() {}

    // ========== GRAB POSITIONS ==========
    
    /** Grab pickup 1 control X position (inches) */
    public static double GRAB_PICKUP1_CONTROL_X = 26;
    
    /** Grab pickup 1 control Y position (inches) */
    public static double GRAB_PICKUP1_CONTROL_Y = 70;
    
    /** Grab pickup 1 X position (inches) */
    public static double GRAB_PICKUP1_X = 28;
    
    /** Grab pickup 1 Y position (inches) */
    public static double GRAB_PICKUP1_Y = 46;
    
    /** Grab pickup 2 X position (inches) */
    public static double GRAB_PICKUP2_X = 28;
    
    /** Grab pickup 2 Y position (inches) */
    public static double GRAB_PICKUP2_Y = 36;
    
    /** Grab pickup 3 X position (inches) */
    public static double GRAB_PICKUP3_X = 28;
    
    /** Grab pickup 3 Y position (inches) */
    public static double GRAB_PICKUP3_Y = 26;
    
    /** Grab score heading (degrees) */
    public static double GRAB_SCORE_HEADING_DEG = 0;
    
    /** Grab specimen X position (inches) */
    public static double GRAB_SPECIMEN_X = 13;

    // ========== PARK POSITIONS ==========
    
    /** Park control X position (inches) */
    public static double PARK_CONTROL_X = 26;
    
    /** Park control Y position (inches) */
    public static double PARK_CONTROL_Y = 70;
    
    /** Park heading (degrees) */
    public static double PARK_HEADING_DEG = 230;
    
    /** Park X position (inches) */
    public static double PARK_X = 20;
    
    /** Park Y position (inches) */
    public static double PARK_Y = 40;

    // ========== PICKUP HEADINGS ==========
    
    /** Pickup 1 heading (degrees) */
    public static double PICKUP1_HEADING_DEG = 310;
    
    /** Pickup 2 heading (degrees) */
    public static double PICKUP2_HEADING_DEG = 310;
    
    /** Pickup 3 heading (degrees) */
    public static double PICKUP3_HEADING_DEG = 310;

    // ========== PREP GRAB POSITIONS ==========
    
    /** Prep and grab specimen Y position (inches) */
    public static double PREP_AND_GRAB_SPECIMEN_Y = 24;
    
    /** Prep grab specimen control X 1 position (inches) */
    public static double PREP_GRAB_SPECIMEN_CONTROL_X_1 = 20;
    
    /** Prep grab specimen control X 2 position (inches) */
    public static double PREP_GRAB_SPECIMEN_CONTROL_X_2 = 50;
    
    /** Prep grab specimen control Y 1 position (inches) */
    public static double PREP_GRAB_SPECIMEN_CONTROL_Y_1 = 70;
    
    /** Prep grab specimen control Y 2 position (inches) */
    public static double PREP_GRAB_SPECIMEN_CONTROL_Y_2 = 24;
    
    /** Prep grab specimen X position (inches) */
    public static double PREP_GRAB_SPECIMEN_X = 17;

    // ========== PUSH POSITIONS ==========
    
    /** Push pickup 1 control X position (inches) */
    public static double PUSH_PICKUP1_CONTROL_X = 70;
    
    /** Push pickup 1 control Y position (inches) */
    public static double PUSH_PICKUP1_CONTROL_Y = 30;
    
    /** Push pickup 1 prep control X position (inches) */
    public static double PUSH_PICKUP1_PREP_CONTROL_X = 33;
    
    /** Push pickup 1 prep control Y position (inches) */
    public static double PUSH_PICKUP1_PREP_CONTROL_Y = 36;
    
    /** Push pickup 1 prep X position (inches) */
    public static double PUSH_PICKUP1_PREP_X = 48;
    
    /** Push pickup 1 prep Y position (inches) */
    public static double PUSH_PICKUP1_PREP_Y = 36;
    
    /** Push pickup 1 X position (inches) */
    public static double PUSH_PICKUP1_X = 48;
    
    /** Push pickup 1 Y position (inches) */
    public static double PUSH_PICKUP1_Y = 28;
    
    /** Push pickup 2 control X position (inches) */
    public static double PUSH_PICKUP2_CONTROL_X = 62;
    
    /** Push pickup 2 control Y position (inches) */
    public static double PUSH_PICKUP2_CONTROL_Y = 28;
    
    /** Push pickup 2 X position (inches) */
    public static double PUSH_PICKUP2_X = 62;
    
    /** Push pickup 2 Y position (inches) */
    public static double PUSH_PICKUP2_Y = 18;
    
    /** Push pickup 3 control X position (inches) */
    public static double PUSH_PICKUP3_CONTROL_X = 62;
    
    /** Push pickup 3 control Y position (inches) */
    public static double PUSH_PICKUP3_CONTROL_Y = 18;
    
    /** Push pickup 3 X position (inches) */
    public static double PUSH_PICKUP3_X = 62;
    
    /** Push pickup 3 Y position (inches) */
    public static double PUSH_PICKUP3_Y = 8;
    
    /** Push retrieve pickup 1 Y position (inches) */
    public static double PUSH_RETRIEVE_PICKUP1_Y = 28;
    
    /** Push retrieve pickup 2 Y position (inches) */
    public static double PUSH_RETRIEVE_PICKUP2_Y = 18;
    
    /** Push retrieve pickup 3 Y position (inches) */
    public static double PUSH_RETRIEVE_PICKUP3_Y = 8;
    
    /** Push retrieve pickup X position (inches) */
    public static double PUSH_RETRIEVE_PICKUP_X = 20;

    // ========== RETRIEVE POSITIONS ==========
    
    /** Retrieve heading (degrees) */
    public static double RETRIEVE_HEADING_DEG = 235;
    
    /** Retrieve sample turret position */
    public static double RETRIEVE_SAMPLE_TURRET_POS = 0.8;

    // ========== SAMPLE POSITIONS ==========
    
    /** Sample 1 turret position */
    public static double SAMPLE1_TURRET_POS = 0.7;
    
    /** Sample 1 wrist position */
    public static double SAMPLE1_WRIST_POS = 0.4;
    
    /** Sample 2 turret position */
    public static double SAMPLE2_TURRET_POS = 0.65;
    
    /** Sample 2 wrist position */
    public static double SAMPLE2_WRIST_POS = 0.8;
    
    /** Sample 3 turret position */
    public static double SAMPLE3_TURRET_POS = 0.8;
    
    /** Sample 3 wrist position */
    public static double SAMPLE3_WRIST_POS = 0.3;

    // ========== SCORE POSITIONS ==========
    
    /** Score 1 Y position (inches) */
    public static double SCORE1_Y = 71;
    
    /** Score 2 Y position (inches) */
    public static double SCORE2_Y = 70;
    
    /** Score 3 Y position (inches) */
    public static double SCORE3_Y = 69;
    
    /** Score 4 Y position (inches) */
    public static double SCORE4_Y = 68;
    
    /** Score control X position (inches) */
    public static double SCORE_CONTROL_X = 13;
    
    /** Score control Y position (inches) */
    public static double SCORE_CONTROL_Y = 70;
    
    /** Score preload Y position (inches) */
    public static double SCORE_PRELOAD_Y = 72;
    
    /** Score X position (inches) */
    public static double SCORE_X = 38;

    // ========== START POSITIONS ==========
    
    /** Start heading (degrees) */
    public static double START_HEADING_DEG = 0;
    
    /** Start X position (inches) */
    public static double START_X = 7;
    
    /** Start Y position (inches) */
    public static double START_Y = 72;
}
