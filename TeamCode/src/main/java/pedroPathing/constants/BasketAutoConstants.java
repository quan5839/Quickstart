package pedroPathing.constants;

import com.acmerobotics.dashboard.config.Config;

/**
 * Autonomous constants specific to basket scoring mode
 * Organized alphabetically within sections
 */
@Config
public class BasketAutoConstants {
    private BasketAutoConstants() {}

    // ========== PARK POSITIONS ==========
    
    /** Park control heading (degrees) */
    public static double PARK_CONTROL_HEADING_DEG = 90;
    
    /** Park control X position (inches) */
    public static double PARK_CONTROL_X = 60;
    
    /** Park control Y position (inches) */
    public static double PARK_CONTROL_Y = 98;
    
    /** Park heading (degrees) */
    public static double PARK_HEADING_DEG = 90;
    
    /** Park X position (inches) */
    public static double PARK_X = 60;
    
    /** Park Y position (inches) */
    public static double PARK_Y = 98;

    // ========== PICKUP POSITIONS ==========
    
    /** Pickup 1 heading (degrees) */
    public static double PICKUP1_HEADING_DEG = 356;
    
    /** Pickup 1 X position (inches) */
    public static double PICKUP1_X = 11.5;
    
    /** Pickup 1 Y position (inches) */
    public static double PICKUP1_Y = 118.55;
    
    /** Pickup 2 heading (degrees) */
    public static double PICKUP2_HEADING_DEG = 354;
    
    /** Pickup 2 X position (inches) */
    public static double PICKUP2_X = 11.2;
    
    /** Pickup 2 Y position (inches) */
    public static double PICKUP2_Y = 128.6;
    
    /** Pickup 3 heading (degrees) */
    public static double PICKUP3_HEADING_DEG = 0;
    
    /** Pickup 3 X position (inches) */
    public static double PICKUP3_X = 14;
    
    /** Pickup 3 Y position (inches) */
    public static double PICKUP3_Y = 131.8;

    // ========== SAMPLE INTAKE POSITIONS ==========
    
    /** Sample 1 turret position */
    public static double SAMPLE1_TURRET_POS = 0.7;
    
    /** Sample 1 wrist position */
    public static double SAMPLE1_WRIST_POS = 0.4;
    
    /** Sample 2 turret position */
    public static double SAMPLE2_TURRET_POS = 0.65;
    
    /** Sample 2 wrist position */
    public static double SAMPLE2_WRIST_POS = 0.4;
    
    /** Sample 3 turret position */
    public static double SAMPLE3_TURRET_POS = 0.8;
    
    /** Sample 3 wrist position */
    public static double SAMPLE3_WRIST_POS = 0.2;

    // ========== SCORE POSITIONS ==========
    
    /** Score heading (degrees) */
    public static double SCORE_HEADING_DEG = 315;
    
    /** Score X position (inches) */
    public static double SCORE_X = 13.5;
    
    /** Score Y position (inches) */
    public static double SCORE_Y = 127;

    // ========== START POSITIONS ==========
    
    /** Start heading (degrees) */
    public static double START_HEADING_DEG = 270;
    
    /** Start X position (inches) */
    public static double START_X = 7;
    
    /** Start Y position (inches) */
    public static double START_Y = 110;
}
