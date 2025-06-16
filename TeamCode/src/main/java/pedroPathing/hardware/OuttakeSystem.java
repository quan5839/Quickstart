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
    private DcMotorEx outtakeSlideRight;
    private DcMotorEx outtakeSlideLeft;
    private Servo outtakeElbowRight;
    private Servo outtakeElbowLeft;
    private Servo outtakeWrist;
    private Servo outtakeClaw;
    private RevColorSensorV3 outtakeColorSensor;
    private ColorDetectionUtil colorDetectionUtil;
    private OpMode myOpMode;
    private DigitalChannel outtakeSlideLimit;

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
        outtakeElbowRight = safeInitServo("outtakeElbowRight", Servo.Direction.REVERSE, OuttakeConstants.OUTTAKE_ELBOW_INIT);
        outtakeElbowLeft = safeInitServo("outtakeElbowLeft", Servo.Direction.FORWARD, OuttakeConstants.OUTTAKE_ELBOW_INIT);
        outtakeWrist = safeInitServo("outtakeWrist", Servo.Direction.FORWARD, OuttakeConstants.OUTTAKE_WRIST_INIT);
        outtakeClaw = safeInitServo("outtakeClaw", Servo.Direction.FORWARD, OuttakeConstants.CLAW_CLOSED, OuttakeConstants.CLAW_OPEN, OuttakeConstants.CLAW_OPEN);
        outtakeSlideRight = myOpMode.hardwareMap.get(DcMotorEx.class, "outtakeSlideRight");
        outtakeSlideLeft = myOpMode.hardwareMap.get(DcMotorEx.class, "outtakeSlideLeft");
        outtakeSlideRight.setDirection(DcMotor.Direction.FORWARD);
        outtakeSlideLeft.setDirection(DcMotor.Direction.REVERSE);
        try {
            outtakeColorSensor = myOpMode.hardwareMap.get(RevColorSensorV3.class, "outtakeColorSensor");
            outtakeColorSensor.enableLed(true);
            colorDetectionUtil = new ColorDetectionUtil(outtakeColorSensor, "outtakeColorSensor");
        } catch (Exception e) {
            myOpMode.telemetry.addData("ERROR", "outtakeColorSensor not found!");
            colorDetectionUtil = new ColorDetectionUtil(null, "outtake");
        }

        // Initialize outtake slide limit switch
        try {
            outtakeSlideLimit = myOpMode.hardwareMap.get(DigitalChannel.class, "outtakeSlideLimitSwitch");
            outtakeSlideLimit.setMode(DigitalChannel.Mode.INPUT);
        } catch (Exception e) {
            myOpMode.telemetry.addData("ERROR", "outtakeSlideLimitSwitch not found!");
            outtakeSlideLimit = null;
        }

        myOpMode.telemetry.update();
    }

    public void setOuttakeElbowPosition(double position) {
        if (outtakeElbowLeft != null) outtakeElbowLeft.setPosition(position);
        if (outtakeElbowRight != null) outtakeElbowRight.setPosition(position);
    }

    public void setOuttakeSlidePosition(int position, double power) {
        outtakeSlideLeft.setTargetPosition(position);
        outtakeSlideRight.setTargetPosition(position);
        outtakeSlideLeft.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_TO_POSITION);
        outtakeSlideRight.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_TO_POSITION);
        outtakeSlideLeft.setPower(power);
        outtakeSlideRight.setPower(power);
    }

    public void setOuttakeSlidePosition(int position) {
        outtakeSlideLeft.setTargetPosition(position);
        outtakeSlideRight.setTargetPosition(position);
        outtakeSlideLeft.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_TO_POSITION);
        outtakeSlideRight.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_TO_POSITION);
        outtakeSlideLeft.setPower(1);
        outtakeSlideRight.setPower(1);
    }

    public void resetOuttakeSlide() {
        outtakeSlideLeft.setPower(0);
        outtakeSlideRight.setPower(0);
        outtakeSlideLeft.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        outtakeSlideRight.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        outtakeSlideLeft.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_USING_ENCODER);
        outtakeSlideRight.setMode(com.qualcomm.robotcore.hardware.DcMotor.RunMode.RUN_USING_ENCODER);
    }

    public void setOuttakeClawPosition(double position) {
        if (outtakeClaw != null) outtakeClaw.setPosition(position);
    }

    public double getOuttakeClawPosition() {
        return outtakeClaw != null ? outtakeClaw.getPosition() : 0;
    }

    public void openOuttakeClaw() {
        if (outtakeClaw != null) outtakeClaw.setPosition(OuttakeConstants.CLAW_OPEN);
    }

    public void closeOuttakeClaw() {
        if (outtakeClaw != null) outtakeClaw.setPosition(OuttakeConstants.CLAW_CLOSED);
    }

    public void setOuttakeWristPosition(double position) {
        if (outtakeWrist != null) outtakeWrist.setPosition(position);
    }

    public double getOuttakeWristPosition() {
        return outtakeWrist != null ? outtakeWrist.getPosition() : 0;
    }

    public void setOuttakeWristGrab() {
        if (outtakeWrist != null) outtakeWrist.setPosition(OuttakeConstants.OUTTAKE_WRIST_GRAB);
    }

    public void setOuttakeWristBasket() {
        if (outtakeWrist != null) outtakeWrist.setPosition(OuttakeConstants.OUTTAKE_WRIST_BASKET);
    }

    public void setOuttakeWristTuck() {
        if (outtakeWrist != null) outtakeWrist.setPosition(OuttakeConstants.OUTTAKE_WRIST_TUCK);
    }

    public void setOuttakeWristGetSpecimen() {
        if (outtakeWrist != null)
            outtakeWrist.setPosition(OuttakeConstants.OUTTAKE_WRIST_GET_SPECIMEN);
    }

    public void setOuttakeWristScoreSpecimen() {
        if (outtakeWrist != null)
            outtakeWrist.setPosition(OuttakeConstants.OUTTAKE_WRIST_SCORE_SPECIMEN);
    }

    public DcMotorEx getOuttakeSlideLeft() { return outtakeSlideLeft; }

    public DcMotorEx getOuttakeSlideRight() { return outtakeSlideRight; }

    public Servo getOuttakeElbowLeft() { return outtakeElbowLeft; }

    public Servo getOuttakeElbowRight() { return outtakeElbowRight; }

    public Servo getOuttakeWrist() { return outtakeWrist; }

    public Servo getOuttakeClaw() { return outtakeClaw; }

    public OuttakeState getState() {
        return currentState;
    }

    public void prepareOuttakeForReceive() {
        currentState = OuttakeState.PREPPING;
        setOuttakeElbowPosition(OuttakeConstants.OUTTAKE_ELBOW_BASE);
        setOuttakeWristGrab();
        openOuttakeClaw();
        currentState = OuttakeState.RECEIVING;
    }

    public void prepareOuttakeForScoring() {
        currentState = OuttakeState.PREPPING;
        setOuttakeElbowPosition(OuttakeConstants.OUTTAKE_ELBOW_SCORE_SPECIMEN);
        setOuttakeWristScoreSpecimen();
        currentState = OuttakeState.SCORING;
    }

    public void prepareOuttakeForBasket() {
        currentState = OuttakeState.PREPPING;
        setOuttakeElbowPosition(OuttakeConstants.OUTTAKE_ELBOW_BASKET);
        setOuttakeWristBasket();
        currentState = OuttakeState.BASKET;
    }

    public void completeOuttake() {
        currentState = OuttakeState.COMPLETE;
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
        if (outtakeSlideLimit == null) return false;
        // Limit switch is "active low" - returns false when pressed
        return !outtakeSlideLimit.getState();
    }
}
