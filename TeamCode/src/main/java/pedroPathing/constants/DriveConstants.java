package pedroPathing.constants;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.hardware.DcMotor;

/**
 * Drive system hardware constants
 * Organized alphabetically within sections
 */
@Config
public class DriveConstants {
    private DriveConstants() {}

    // ========== MOTOR DIRECTIONS ==========
    
    /** Back left motor direction */
    public static final DcMotor.Direction BACK_LEFT_DIRECTION = DcMotor.Direction.REVERSE;
    
    /** Back right motor direction */
    public static final DcMotor.Direction BACK_RIGHT_DIRECTION = DcMotor.Direction.FORWARD;
    
    /** Front left motor direction */
    public static final DcMotor.Direction FRONT_LEFT_DIRECTION = DcMotor.Direction.REVERSE;
    
    /** Front right motor direction */
    public static final DcMotor.Direction FRONT_RIGHT_DIRECTION = DcMotor.Direction.FORWARD;

    // ========== MOTOR NAMES ==========
    
    /** Back left motor hardware map name */
    public static final String BACK_LEFT_NAME = "backLeft";
    
    /** Back right motor hardware map name */
    public static final String BACK_RIGHT_NAME = "backRight";
    
    /** Front left motor hardware map name */
    public static final String FRONT_LEFT_NAME = "frontLeft";
    
    /** Front right motor hardware map name */
    public static final String FRONT_RIGHT_NAME = "frontRight";
}
