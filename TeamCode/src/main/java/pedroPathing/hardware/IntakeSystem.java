package pedroPathing.hardware;

import com.qualcomm.hardware.rev.RevColorSensorV3;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.Servo;

import pedroPathing.constants.IntakeConstants;
import pedroPathing.util.ColorDetectionUtil;

public class IntakeSystem {
    private Servo turret;
    private Servo slideRight;
    private Servo slideLeft;
    private Servo shoulderRight;
    private Servo shoulderLeft;
    private Servo elbow;
    private Servo wrist;
    private Servo claw;
    private RevColorSensorV3 colorSensor;
    private DigitalChannel slideLimit;
    private ColorDetectionUtil colorUtil;
    private OpMode opMode;

    public IntakeSystem(OpMode opMode) {
        this.opMode = opMode;
    }

    // Helper function for initializing servos safely
    private Servo safeInitServo(String name, Servo.Direction direction, Double servoMin, Double servoMax, Double position) {
        Servo servo = null;
        try {
            servo = opMode.hardwareMap.get(Servo.class, name);
            if (servoMin != null && servoMax != null) servo.scaleRange(servoMin, servoMax);
            if (direction != null) servo.setDirection(direction);
            if (position != null) servo.setPosition(position);
        } catch (Exception e) {
            opMode.telemetry.addData("ERROR", name + " not found!");
        }
        return servo;
    }
    private Servo safeInitServo(String name, Servo.Direction direction, Double position) {
        return safeInitServo(name, direction, null, null, position);
    }

    public void init() {
        slideRight = safeInitServo("intakeSlideRight", Servo.Direction.REVERSE, IntakeConstants.SLIDE_MIN);
        slideLeft = safeInitServo("intakeSlideLeft", Servo.Direction.FORWARD, IntakeConstants.SLIDE_MIN);
        turret = safeInitServo("intakeTurret", Servo.Direction.FORWARD, IntakeConstants.TURRET_MIN_OFFSET, IntakeConstants.TURRET_MAX_OFFSET, IntakeConstants.TURRET_MIDDLE);
        shoulderRight = safeInitServo("intakeShoulderRight", Servo.Direction.REVERSE, IntakeConstants.SHOULDER_INIT);
        shoulderLeft = safeInitServo("intakeShoulderLeft", Servo.Direction.FORWARD, IntakeConstants.SHOULDER_INIT);
        elbow = safeInitServo("intakeElbow", Servo.Direction.FORWARD, IntakeConstants.ELBOW_INIT);
        wrist = safeInitServo("intakeWrist", Servo.Direction.FORWARD, IntakeConstants.WRIST_MIDDLE);
        claw = safeInitServo("intakeClaw", Servo.Direction.FORWARD, IntakeConstants.CLAW_OPEN);
        try {
            colorSensor = opMode.hardwareMap.get(RevColorSensorV3.class, "intakeColorSensor");
            colorSensor.enableLed(true);
            colorUtil = new ColorDetectionUtil(colorSensor, "intake");
        } catch (Exception e) {
            opMode.telemetry.addData("ERROR", "intakeColorSensor not found!");
            colorUtil = new ColorDetectionUtil(null, "intake");
        }

        try {
            slideLimit = opMode.hardwareMap.get(DigitalChannel.class, "intakeSlideLimitSwitch");
            slideLimit.setMode(DigitalChannel.Mode.INPUT);
        } catch (Exception e) {
            opMode.telemetry.addData("ERROR", "intakeSlideLimitSwitch not found!");
            slideLimit = null;
        }

        opMode.telemetry.update();
    }

    public boolean isSampleDetected(double position) {
        return colorUtil.isSampleDetected();
    }

    public SampleColor getSampleColor() {
        return colorUtil.getSampleColor();
    }

    public double[] getRawRGBValues() {
        return colorUtil.getRawRGBValues();
    }

    public boolean isSamplePresent() {
        return colorUtil.isSamplePresent();
    }

    public boolean isValidSampleDetected() {
        if (colorUtil == null) return false;

        double distance = colorUtil.getDistance();
        if (distance < 0 || distance > pedroPathing.constants.ControlConstants.COLOR_DETECTION_MAX_DISTANCE_CM) {
            return false;
        }

        SampleColor detectedColor = colorUtil.getSampleColor();
        return detectedColor != SampleColor.NONE;
    }

    /**
     * Get the distance reading from the intake color sensor
     * @return Distance in CM, or -1 if sensor unavailable
     */
    public double getIntakeDistance() {
        if (colorUtil == null) return -1;
        return colorUtil.getDistance();
    }

    /**
     * Check if the intake slide has reached the hold position using the mechanical limit switch
     * Also returns true if slide is already at a higher position (more retracted) than SLIDE_HOLD
     * @return true if the limit switch is pressed (slide at hold position) or slide is more retracted, false otherwise
     */
    public boolean isSlideAtHoldPosition() {
        if (slideLimit != null) {
            boolean limitSwitchPressed = !slideLimit.getState();
            if (limitSwitchPressed) {
                return true;
            }
        }

        double currentPosition = getIntakeSlidePosition();
        return currentPosition >= IntakeConstants.SLIDE_HOLD;
    }

    public double getIntakeElbowPosition() {
        return elbow.getPosition();
    }

    public double getIntakeShoulderPosition() {
        if (shoulderLeft != null && shoulderRight != null) {
            return (shoulderLeft.getPosition() + shoulderRight.getPosition()) / 2.0;
        } else if (shoulderLeft != null) {
            return shoulderLeft.getPosition();
        } else if (shoulderRight != null) {
            return shoulderRight.getPosition();
        } else {
            return 0;
        }
    }


    public void setIntakeElbowPosition(double position) {
        if (elbow != null){
            elbow.setPosition(position);
        }
    }

    public boolean setIntakeElbowPositionSmart(double position) {
        if (elbow != null && opMode instanceof pedroPathing.BaseTeleop25152) {
            elbow.setPosition(position);
            return true;
        }
        return false;
    }

    public void setIntakeShoulderPosition(double position) {
        if (shoulderLeft != null) {
            shoulderLeft.setPosition(position);
        }
        if (shoulderRight != null) {
            shoulderRight.setPosition(position);
        }
    }

    public void setIntakeSlidePosition(double position) {
        if (slideLeft != null) {
            slideLeft.setPosition(position);
        }
        if (slideRight != null) {
            slideRight.setPosition(position);
        }
    }

    public void setIntakeClawPosition(double position) {
        if (claw != null) {
            claw.setPosition(position);
        }
    }

    public double getIntakeClawPosition() {
        if (claw != null) {
            return claw.getPosition();
        } else {
            return 0;
        }
    }

    public void openIntakeClaw() {
        if (claw != null) {
            claw.setPosition(IntakeConstants.CLAW_OPEN);
        }
    }

    public void closeIntakeClaw() {
        if (claw != null) {
            claw.setPosition(IntakeConstants.CLAW_CLOSED);
        }
    }

    public void setIntakeWristPosition(double position) {
        if (wrist != null) {
            wrist.setPosition(position);
        }
    }

    public double getIntakeWristPosition() {
        if (wrist != null) {
            return wrist.getPosition();
        } else {
            return 0;
        }
    }

    public void setIntakeWristBase() {
        if (wrist != null) {
            wrist.setPosition(IntakeConstants.WRIST_MIDDLE);
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
        if (turret != null) {
            turret.setPosition(position);
        }
    }

    public double getIntakeTurretPosition() {
        if (turret != null) {
            return turret.getPosition();
        } else {
            return 0;
        }
    }

    public Servo getIntakeTurret() {
        return turret;
    }

    public Servo getIntakeSlideLeft() {
        return slideLeft;
    }

    public Servo getIntakeSlideRight() {
        return slideRight;
    }

    public Servo getIntakeElbow() {
        return elbow;
    }

    public Servo getIntakeShoulderLeft() {
        return shoulderLeft;
    }

    public Servo getIntakeShoulderRight() {
        return shoulderRight;
    }

    public Servo getIntakeWrist() {
        return wrist;
    }

    public Servo getIntakeClaw() {
        return claw;
    }

    public RevColorSensorV3 getColorSensor() {
        return colorSensor;
    }
}
