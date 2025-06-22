package pedroPathing.constants;

import com.acmerobotics.dashboard.config.Config;

/**
 * Shared autonomous constants used across multiple autonomous modes
 * Organized alphabetically within sections
 */
@Config
public class AutoConstants {
    private AutoConstants() {}

    // ========== SHARED TIMING CONSTANTS ==========
    
    /** Intake slide wait time (ms) */
    public static long INTAKE_SLIDE_WAIT_TIME = 2300;
    
    /** Outtake claw release time (ms) */
    public static long OUTTAKE_CLAW_RELEASE = 1500;
}
