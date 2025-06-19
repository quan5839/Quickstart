package pedroPathing.hardware;

import com.qualcomm.hardware.rev.RevColorSensorV3;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.Servo;

import pedroPathing.constants.OuttakeConstants;
import pedroPathing.util.ColorDetectionUtil;

public class OuttakeSystem {
    private DcMotorEx slideRight;
    private DcMotorEx slideLeft;
    private Servo elbowRight;
    private Servo elbowLeft;
    private Servo wrist;
    private Servo claw;
    private RevColorSensorV3 colorSensor;
    private ColorDetectionUtil colorDetectionUtil;
    private OpMode myOpMode;
    private DigitalChannel slideLimit;

    // Track the logical state of the subsystem
    public enum OuttakeState {IDLE, PREPPING, RECEIVING, SCORING, BASKET, COMPLETE}

    private OuttakeState currentState = OuttakeState.IDLE;

    public OuttakeSystem(OpMode opMode) {
        this.myOpMode = opMode;
    }

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
        elbowRight = safeInitServo("outtakeElbowRight", Servo.Direction.REVERSE, OuttakeConstants.ELBOW_INIT);
        elbowLeft = safeInitServo("outtakeElbowLeft", Servo.Direction.FORWARD, OuttakeConstants.ELBOW_INIT);
        wrist = safeInitServo("outtakeWrist", Servo.Direction.FORWARD, OuttakeConstants.WRIST_INIT);
        claw = safeInitServo("outtakeClaw", Servo.Direction.FORWARD, OuttakeConstants.CLAW_OPEN);
        slideRight = myOpMode.hardwareMap.get(DcMotorEx.class, "outtakeSlideRight");
        slideLeft = myOpMode.hardwareMap.get(DcMotorEx.class, "outtakeSlideLeft");
        slideRight.setDirection(DcMotor.Direction.FORWARD);
        slideLeft.setDirection(DcMotor.Direction.REVERSE);
        try {
            colorSensor = myOpMode.hardwareMap.get(RevColorSensorV3.class, "outtakeColorSensor");
            colorSensor.enableLed(true);
            colorDetectionUtil = new ColorDetectionUtil(colorSensor, "outtakeColorSensor");
        } catch (Exception e) {
            myOpMode.telemetry.addData("ERROR", "outtakeColorSensor not found!");
            colorDetectionUtil = new ColorDetectionUtil(null, "outtake");
        }

        // Initialize outtake slide limit switch
        try {
            slideLimit = myOpMode.hardwareMap.get(DigitalChannel.class, "outtakeSlideLimitSwitch");
            slideLimit.setMode(DigitalChannel.Mode.INPUT);
        } catch (Exception e) {
            myOpMode.telemetry.addData("ERROR", "outtakeSlideLimitSwitch not found!");
            slideLimit = null;
        }

        myOpMode.telemetry.update();
    }

    public void setElbowPosition(double position) {
        if (elbowLeft != null) elbowLeft.setPosition(position);
        if (elbowRight != null) elbowRight.setPosition(position);
    }

    public void setSlidePosition(int position, double power) {
        slideLeft.setTargetPosition(position);
        slideRight.setTargetPosition(position);
        slideLeft.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_TO_POSITION);
        slideRight.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_TO_POSITION);
        slideLeft.setPower(power);
        slideRight.setPower(power);
    }

    public void setSlidePosition(int position) {
        slideLeft.setTargetPosition(position);
        slideRight.setTargetPosition(position);
        slideLeft.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_TO_POSITION);
        slideRight.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_TO_POSITION);
        slideLeft.setPower(1);
        slideRight.setPower(1);
    }

    public void resetSlide() {
        slideLeft.setPower(0);
        slideRight.setPower(0);
        slideLeft.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        slideRight.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        slideLeft.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_USING_ENCODER);
        slideRight.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_USING_ENCODER);
    }

    public void setClawPosition(double position) {
        if (claw != null) claw.setPosition(position);
    }

    public void setWristPosition(double position) {
        if (wrist != null) wrist.setPosition(position);
    }

    public double getOuttakeClawPosition() {
        return claw != null ? claw.getPosition() : 0;
    }

    public void openClaw() {
        if (claw != null) claw.setPosition(OuttakeConstants.CLAW_OPEN);
    }

    public void openFullyClaw(){
        if (claw != null) claw.setPosition(OuttakeConstants.CLAW_FULLY_OPEN);
    }

    public void closeClaw() {
        if (claw != null) claw.setPosition(OuttakeConstants.CLAW_CLOSED);
    }



    public DcMotorEx getSlideLeft() { return slideLeft; }

    public DcMotorEx getSlideRight() { return slideRight; }

    public Servo getElbowLeft() { return elbowLeft; }

    public Servo getElbowRight() { return elbowRight; }

    public Servo getWrist() { return wrist; }

    public Servo getClaw() { return claw; }

    public OuttakeState getState() {
        return currentState;
    }


    public boolean isSampleDetected(double position) {
        return colorDetectionUtil.isSampleDetected();
    }

    public SampleColor getSampleColor() {
        return colorDetectionUtil.getSampleColor();
    }

    /**
     * Check if a specimen is detected based on distance only (optimized for specimen detection)
     * Uses a larger distance threshold than sample detection for specimen pickup
     * @return true if sensor detects something within specimen detection range
     */
    public boolean isSpecimenDetected() {
        if (colorDetectionUtil == null) return false;
        double distance = colorDetectionUtil.getDistance();
        return distance > 0 && distance < pedroPathing.constants.ControlConstants.SPECIMEN_DETECTION_DISTANCE_CM;
    }

    /**
     * Get the distance reading from the outtake color sensor
     * @return Distance in CM, or -1 if sensor unavailable
     */
    public double getOuttakeDistance() {
        if (colorDetectionUtil == null) return -1;
        return colorDetectionUtil.getDistance();
    }

    public boolean isSamplePresent() {
        return colorDetectionUtil.isSamplePresent();
    }

    /**
     * Check if the outtake slide has reached the minimum position using the mechanical limit switch
     * @return true if the limit switch is pressed (slide at minimum position), false otherwise
     */
    public boolean isSlideAtMinPosition() {
        if (slideLimit == null) return false;
        // Limit switch is "active low" - returns false when pressed
        return !slideLimit.getState();
    }
}
