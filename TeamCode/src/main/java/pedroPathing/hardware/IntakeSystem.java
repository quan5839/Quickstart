package pedroPathing.hardware;

import com.qualcomm.hardware.rev.RevColorSensorV3;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.Servo;

import pedroPathing.constants.IntakeConstants;
import pedroPathing.util.ColorDetectionUtil;

public class IntakeSystem {
    private Servo intakeTurret;
    private Servo intakeSlideRight;
    private Servo intakeSlideLeft;
    private Servo intakeShoulderRight;
    private Servo intakeShoulderLeft;
    private Servo intakeElbow;
    private Servo intakeWrist;
    private Servo intakeClaw;
    private RevColorSensorV3 intakeColorSensor;
    private DigitalChannel intakeSlideLimit; // Limit switch for slide hold position
    private ColorDetectionUtil colorDetectionUtil;
    private OpMode myOpMode;

    public IntakeSystem(OpMode opMode) {
        this.myOpMode = opMode;
    }

    // Helper function for initializing servos safely
    private Servo safeInitServo(String name, Servo.Direction direction, Double servoMin, Double servoMax, Double position) {
        Servo servo = null;
        try {
            servo = myOpMode.hardwareMap.get(Servo.class, name);
            if (servoMin != null && servoMax != null) servo.scaleRange(servoMin, servoMax);
            if (direction != null) servo.setDirection(direction);
            if (position != null) servo.setPosition(position);
        } catch (Exception e) {
            myOpMode.telemetry.addData("ERROR", name + " not found!");
        }
        return servo;
    }
    private Servo safeInitServo(String name, Servo.Direction direction, Double position) {
        return safeInitServo(name, direction, null, null, position);
    }

    public void init() {
        intakeSlideRight = safeInitServo("intakeSlideRight", Servo.Direction.REVERSE, IntakeConstants.SLIDE_MIN);
        intakeSlideLeft = safeInitServo("intakeSlideLeft", Servo.Direction.FORWARD, IntakeConstants.SLIDE_MIN);
        intakeTurret = safeInitServo("intakeTurret", Servo.Direction.FORWARD, IntakeConstants.TURRET_MIN_OFFSET, IntakeConstants.TURRET_MAX_OFFSET, IntakeConstants.TURRET_MIDDLE);
        intakeShoulderRight = safeInitServo("intakeShoulderRight", Servo.Direction.REVERSE, IntakeConstants.SHOULDER_INIT);
        intakeShoulderLeft = safeInitServo("intakeShoulderLeft", Servo.Direction.FORWARD, IntakeConstants.SHOULDER_INIT);
        intakeElbow = safeInitServo("intakeElbow", Servo.Direction.FORWARD, IntakeConstants.ELBOW_INIT);
        intakeWrist = safeInitServo("intakeWrist", Servo.Direction.FORWARD, IntakeConstants.WRIST_MIDDLE);
        intakeClaw = safeInitServo("intakeClaw", Servo.Direction.FORWARD, IntakeConstants.CLAW_OPEN);
        try {
            intakeColorSensor = myOpMode.hardwareMap.get(RevColorSensorV3.class, "intakeColorSensor");
            intakeColorSensor.enableLed(true);
            colorDetectionUtil = new ColorDetectionUtil(intakeColorSensor, "intake");
        } catch (Exception e) {
            myOpMode.telemetry.addData("ERROR", "intakeColorSensor not found!");
            colorDetectionUtil = new ColorDetectionUtil(null, "intake");
        }

        // Initialize intake slide limit switch
        try {
            intakeSlideLimit = myOpMode.hardwareMap.get(DigitalChannel.class, "intakeSlideLimitSwitch");
            intakeSlideLimit.setMode(DigitalChannel.Mode.INPUT);
        } catch (Exception e) {
            myOpMode.telemetry.addData("ERROR", "intakeSlideLimitSwitch not found!");
            intakeSlideLimit = null;
        }


        myOpMode.telemetry.update();
        myOpMode.telemetry.update();
    }

    public boolean isSampleDetected(double position) {
        return colorDetectionUtil.isSampleDetected();
    }

    public SampleColor getSampleColor() {
        return colorDetectionUtil.getSampleColor();
    }

    /**
     * Get raw RGB values for debugging - only call when needed to avoid performance impact
     */
    public double[] getRawRGBValues() {
        return colorDetectionUtil.getRawRGBValues();
    }

    public boolean isSamplePresent() {
        return colorDetectionUtil.isSamplePresent();
    }

    /**
     * Formal color detection with distance validation for intake system
     * This provides a more robust color check similar to the outtake system
     * @return true if a valid sample color is detected within proper distance
     */
    public boolean isValidSampleDetected() {
        if (colorDetectionUtil == null) return false;

        // First check distance to ensure we have something close enough
        double distance = colorDetectionUtil.getDistance();
        if (distance < 0 || distance > pedroPathing.constants.ControlConstants.COLOR_DETECTION_MAX_DISTANCE_CM) {
            return false;
        }

        // Then check if we can detect a valid color
        SampleColor detectedColor = colorDetectionUtil.getSampleColor();
        return detectedColor != SampleColor.NONE;
    }

    /**
     * Get the distance reading from the intake color sensor
     * @return Distance in CM, or -1 if sensor unavailable
     */
    public double getIntakeDistance() {
        if (colorDetectionUtil == null) return -1;
        return colorDetectionUtil.getDistance();
    }

    /**
     * Check if the intake slide has reached the hold position using the mechanical limit switch
     * Also returns true if slide is already at a higher position (more retracted) than SLIDE_HOLD
     * @return true if the limit switch is pressed (slide at hold position) or slide is more retracted, false otherwise
     */
    public boolean isSlideAtHoldPosition() {
        // First check if limit switch is triggered (most reliable)
        if (intakeSlideLimit != null) {
            // Limit switch is "active low" - returns false when pressed
            boolean limitSwitchPressed = !intakeSlideLimit.getState();
            if (limitSwitchPressed) {
                return true;
            }
        }

        // Fallback: check if slide position is at or more retracted than SLIDE_HOLD
        // SLIDE_HOLD = 1.0, so any position >= SLIDE_HOLD means we're at or past the hold position
        double currentPosition = getIntakeSlidePosition();
        return currentPosition >= IntakeConstants.SLIDE_HOLD;
    }

    public double getIntakeElbowPosition() {
        return intakeElbow.getPosition();
    }

    /**
     * Returns the average position of both intake shoulder servos (left and right) if present, otherwise 0.
     */
    public double getIntakeShoulderPosition() {
        if (intakeShoulderLeft != null && intakeShoulderRight != null) {
            return (intakeShoulderLeft.getPosition() + intakeShoulderRight.getPosition()) / 2.0;
        } else if (intakeShoulderLeft != null) {
            return intakeShoulderLeft.getPosition();
        } else if (intakeShoulderRight != null) {
            return intakeShoulderRight.getPosition();
        } else {
            return 0;
        }
    }


    public void setIntakeElbowPosition(double position) {
        if (intakeElbow != null){
            intakeElbow.setPosition(position);
        }
    }

    /**
     * Smart servo position setter that uses caching to reduce I2C traffic
     * @param position Target position for intake elbow
     * @return true if position was actually written to servo
     */
    public boolean setIntakeElbowPositionSmart(double position) {
        if (intakeElbow != null && myOpMode instanceof pedroPathing.BaseTeleop25152) {
            pedroPathing.BaseTeleop25152 teleop = (pedroPathing.BaseTeleop25152) myOpMode;
            // Note: This would require exposing robot hardware from BaseTeleop
            // For now, we'll use the standard method
            intakeElbow.setPosition(position);
            return true;
        }
        return false;
    }

    public void setIntakeShoulderPosition(double position) {
        if (intakeShoulderLeft != null) {
            intakeShoulderLeft.setPosition(position);
        }
        if (intakeShoulderRight != null) {
            intakeShoulderRight.setPosition(position);
        }
    }

    public void setIntakeSlidePosition(double position) {
        if (intakeSlideLeft != null) {
            intakeSlideLeft.setPosition(position);
        }
        if (intakeSlideRight != null) {
            intakeSlideRight.setPosition(position);
        }
    }

    public void setIntakeClawPosition(double position) {
        if (intakeClaw != null) {
            intakeClaw.setPosition(position);
        }
    }

    public double getIntakeClawPosition() {
        if (intakeClaw != null) {
            return intakeClaw.getPosition();
        } else {
            return 0;
        }
    }

    public void openIntakeClaw() {
        if (intakeClaw != null) {
            intakeClaw.setPosition(IntakeConstants.CLAW_OPEN);
        }
    }

    public void closeIntakeClaw() {
        if (intakeClaw != null) {
            intakeClaw.setPosition(IntakeConstants.CLAW_CLOSED);
        }
    }

    public void setIntakeWristPosition(double position) {
        if (intakeWrist != null) {
            intakeWrist.setPosition(position);
        }
    }

    public double getIntakeWristPosition() {
        if (intakeWrist != null) {
            return intakeWrist.getPosition();
        } else {
            return 0;
        }
    }

    public void setIntakeWristBase() {
        if (intakeWrist != null) {
            intakeWrist.setPosition(IntakeConstants.WRIST_MIDDLE);
        }
    }

    public double getIntakeSlidePosition() {
        Servo right = getIntakeSlideRight();
        Servo left = getIntakeSlideLeft();
        if (right != null && left != null)
            return (right.getPosition() + left.getPosition()) / 2.0;
        else if (right != null)
            return right.getPosition();
        else if (left != null)
            return left.getPosition();
        return 0;
    }

    public void setIntakeTurretPosition(double position) {
        if (intakeTurret != null) {
            intakeTurret.setPosition(position);
        }
    }

    public double getIntakeTurretPosition() {
        if (intakeTurret != null) {
            return intakeTurret.getPosition();
        } else {
            return 0;
        }
    }

    public Servo getIntakeTurret() {
        return intakeTurret;
    }

    public Servo getIntakeSlideLeft() {
        return intakeSlideLeft;
    }

    public Servo getIntakeSlideRight() {
        return intakeSlideRight;
    }

    public Servo getIntakeElbow() { return intakeElbow; }

    public Servo getIntakeShoulderLeft() {
        return intakeShoulderLeft;
    }

    public Servo getIntakeShoulderRight() {
        return intakeShoulderRight;
    }

    public Servo getIntakeWrist() {
        return intakeWrist;
    }

    public Servo getIntakeClaw() {
        return intakeClaw;
    }

    public RevColorSensorV3 getColorSensor() {
        return intakeColorSensor;
    }
}
