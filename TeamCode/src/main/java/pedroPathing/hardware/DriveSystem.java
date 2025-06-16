package pedroPathing.hardware;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

import pedroPathing.constants.ControlConstants;

public class DriveSystem {
    private final DcMotorEx frontLeft, frontRight, backLeft, backRight;
    private final VoltageSensor voltageSensor;
    private static final double NOMINAL_VOLTAGE = 12.0;

    private boolean autoOrientEnabled = false;
    private double targetHeadingRad = 0.0;

    private double lastHeadingError = 0;
    private double lastTime = System.currentTimeMillis() / 1000.0;
    private boolean turnOffOrientationLock = false;
    private boolean orientationLock = false;



    private double lastTurnReleasedTime = 0.0; // Seconds since program start
    private boolean pendingHeadingLock = false;

    public enum Orientation {
        FORWARD(0),
        RIGHT(-90),
        LEFT(90),
        BACK(180),
        BASKET(-45),
        CUSTOM(0); // 'CUSTOM' will use runtime value and not angleDeg.

        public final double angleDeg;

        Orientation(double angleDeg) {
            this.angleDeg = angleDeg;
        }

        public double getRadians() {
            return Math.toRadians(angleDeg);
        }
    }

    private Orientation currentOrientation = Orientation.FORWARD;
    // Store custom heading for the CUSTOM orientation
    private double customHeadingDeg = 0.0;

    public void setCustomHeadingDeg(double hdg) {
        customHeadingDeg = hdg;
    }

    public double getCustomHeadingDeg() {
        return customHeadingDeg;
    }

    public double getCustomHeadingRad() {
        return Math.toRadians(customHeadingDeg);
    }

    public void setCustomHeadingRad(double hdgRad) {
        customHeadingDeg = Math.toDegrees(hdgRad);
    }

    public boolean activeHeadingHoldEnabled = true;
    private double lastActiveHeadingError = 0.0;
    private double lastActiveHeadingTime = System.currentTimeMillis() / 1000.0;
    private boolean wasUserTurning = false;

    private boolean precisionModeActive = false; // Internal state for precision mode

    // Movement detection for smart IMU caching
    private static final double MOVEMENT_THRESHOLD = 0.05;
    private double lastHeadingForImpactDetection = 0.0;
    private static final double IMPACT_DETECTION_THRESHOLD = Math.toRadians(5); // 5 degrees sudden change

    // Performance optimization: Motor power batching to reduce hardware calls
    private double lastFrontLeftPower = Double.NaN;
    private double lastBackLeftPower = Double.NaN;
    private double lastFrontRightPower = Double.NaN;
    private double lastBackRightPower = Double.NaN;
    private static final double MOTOR_POWER_TOLERANCE = 0.001;

    public DriveSystem(HardwareMap hardwareMap,
                       String frontLeftName,
                       String frontRightName,
                       String backLeftName,
                       String backRightName) {
        frontLeft = hardwareMap.get(DcMotorEx.class, frontLeftName);
        backLeft = hardwareMap.get(DcMotorEx.class, backLeftName);
        frontRight = hardwareMap.get(DcMotorEx.class, frontRightName);
        backRight = hardwareMap.get(DcMotorEx.class, backRightName);

        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.REVERSE);
        frontRight.setDirection(DcMotor.Direction.FORWARD);
        backRight.setDirection(DcMotor.Direction.FORWARD);

        frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        voltageSensor = hardwareMap.voltageSensor.iterator().hasNext()
                ? hardwareMap.voltageSensor.iterator().next() : null;
    }

    // Performance optimization: Smart motor power setting - only write if power changed
    private void setMotorPowersSmart(double frontLeftPower, double backLeftPower,
                                   double frontRightPower, double backRightPower) {
        if (Double.isNaN(lastFrontLeftPower) ||
            Math.abs(frontLeftPower - lastFrontLeftPower) > MOTOR_POWER_TOLERANCE) {
            frontLeft.setPower(frontLeftPower);
            lastFrontLeftPower = frontLeftPower;
        }

        if (Double.isNaN(lastBackLeftPower) ||
            Math.abs(backLeftPower - lastBackLeftPower) > MOTOR_POWER_TOLERANCE) {
            backLeft.setPower(backLeftPower);
            lastBackLeftPower = backLeftPower;
        }

        if (Double.isNaN(lastFrontRightPower) ||
            Math.abs(frontRightPower - lastFrontRightPower) > MOTOR_POWER_TOLERANCE) {
            frontRight.setPower(frontRightPower);
            lastFrontRightPower = frontRightPower;
        }

        if (Double.isNaN(lastBackRightPower) ||
            Math.abs(backRightPower - lastBackRightPower) > MOTOR_POWER_TOLERANCE) {
            backRight.setPower(backRightPower);
            lastBackRightPower = backRightPower;
        }
    }

    public void drive(Gamepad gamepad1, double headingRad, double currentTimeSeconds) {
        // Read gamepad inputs directly - Android SDK is already efficient
        double forwardInput = -gamepad1.right_stick_y;
        double strafeInput = gamepad1.right_stick_x;
        double rightTrigger = gamepad1.right_trigger;
        double leftTrigger = gamepad1.left_trigger;
        boolean leftBumper = gamepad1.left_bumper;

        double turnInputFromTriggers = rightTrigger - leftTrigger;

        // Determine precision mode internally
        precisionModeActive = leftBumper;

        // Apply precision mode scaling
        double scale = precisionModeActive ? ControlConstants.PRECISION_MODE_SCALE : 1.0;
        double x = strafeInput * scale * ControlConstants.STRAFE_SCALING_FACTOR;
        double y = forwardInput * scale;
        double turn = gamepad1.left_stick_x * scale;
        double userTurnInput = turn; // Save for active heading hold logic

        // Cache voltage (read once per cycle) with performance optimization
        double voltageNorm = 1.0;
        if (voltageSensor != null) {
            double voltage = voltageSensor.getVoltage();
            voltageNorm = Math.min(ControlConstants.NOMINAL_VOLTAGE / voltage, ControlConstants.MIN_VOLTAGE_COMPENSATION);
        }



        // Disable orientation lock if turn input is provided
        if (Math.abs(turn) > ControlConstants.AUTO_ORIENTATION_OVERRIDE_THRESHOLD && orientationLock) {
            disableOrientationLock();
        }

        // Calculate magnitude and direction of movement
        double magnitude = Math.hypot(x, y);
        final double ORIENTATION_CORRECTION_DRIVE_THRESHOLD = ControlConstants.MIN_DRIVE_POWER * 1.01;

        // Always calculate fresh trigonometric values for smooth field-centric movement
        double cosMinusH = Math.cos(-headingRad);
        double sinMinusH = Math.sin(-headingRad);

        // ---------- TURN INPUT LOGIC (mutually exclusive) ----------
        if (orientationLock && magnitude > ORIENTATION_CORRECTION_DRIVE_THRESHOLD) {
            if (Math.abs(turn) > ControlConstants.AUTO_ORIENTATION_OVERRIDE_THRESHOLD) {
                disableOrientationLock();
            } else {
                double targetHeadingRad = (currentOrientation == Orientation.CUSTOM) ? getCustomHeadingRad() : currentOrientation.getRadians();
                double headingError = AngleUnit.normalizeRadians(headingRad - targetHeadingRad);
                double dt = currentTimeSeconds - lastTime;
                double headingErrorDerivative = (headingError - lastHeadingError) / (dt > 0 ? dt : 1e-3);
                turn = headingError * ControlConstants.AUTO_ORIENTATION_P_GAIN + headingErrorDerivative * ControlConstants.AUTO_ORIENTATION_D_GAIN;
                lastHeadingError = headingError;
                lastTime = currentTimeSeconds;
            }
        }
        else if (activeHeadingHoldEnabled) {
            if (!ControlConstants.ENABLE_ACTIVE_HEADING_CORRECTION) {
                // Bypass correction logic if disabled
                frontLeft.setPower(0);
                frontRight.setPower(0);
                backLeft.setPower(0);
                backRight.setPower(0);
                return;
            }
            double customTargetRad = getCustomHeadingRad();
            double headingError = AngleUnit.normalizeRadians(headingRad - customTargetRad);
            double headingErrorDeg = Math.abs(Math.toDegrees(headingError));
            if (headingErrorDeg > ControlConstants.ACTIVE_HEADING_HOLD_DEADBAND_DEG) {
                double dt = currentTimeSeconds - lastActiveHeadingTime;
                double headingErrorDerivative = (headingError - lastActiveHeadingError) / (dt > 0 ? dt : 1e-3);
                double correction = headingError * ControlConstants.ACTIVE_HEADING_P_GAIN
                        + headingErrorDerivative * ControlConstants.ACTIVE_HEADING_D_GAIN;
                if (Math.abs(correction) > 0 && Math.abs(correction) < ControlConstants.ACTIVE_HEADING_MIN_TURN_POWER) {
                    correction = Math.copySign(ControlConstants.ACTIVE_HEADING_MIN_TURN_POWER, correction);
                }
                turn = correction;
                lastActiveHeadingError = headingError;
                lastActiveHeadingTime = currentTimeSeconds;
            }
        }

        // ---------- Active heading/correction state machine ----------
        boolean orientationFeatureActive = orientationLock || autoOrientEnabled;
        boolean userTurning = Math.abs(userTurnInput) > ControlConstants.ACTIVE_HEADING_TURN_DEADBAND;
        boolean allowActiveHeadingCorrection = ControlConstants.ENABLE_ACTIVE_HEADING_CORRECTION && !orientationFeatureActive;
        if (allowActiveHeadingCorrection) {
            if (!userTurning && wasUserTurning) {
                // Just stopped turning: arm heading lock with delay
                lastTurnReleasedTime = currentTimeSeconds;
                pendingHeadingLock = true;
            } else if (userTurning) {
                activeHeadingHoldEnabled = false;
                pendingHeadingLock = false;
            }

            // After 0.1s, if still not turning, engage heading hold to current angle (only once)
            if (pendingHeadingLock && !userTurning && (currentTimeSeconds - lastTurnReleasedTime >= 0.1)) {
                if (!activeHeadingHoldEnabled) { // Only initialize if not already holding
                    setCustomHeadingRad(headingRad);
                    activeHeadingHoldEnabled = true;
                    lastActiveHeadingError = 0.0;
                    lastActiveHeadingTime = currentTimeSeconds;
                }
                pendingHeadingLock = false; // Always clear pending after check
            }
        } else {
            activeHeadingHoldEnabled = false;
            pendingHeadingLock = false;
        }
        wasUserTurning = userTurning;

        if (magnitude > 0 || Math.abs(turn) > 0) {
            double rotX = x * cosMinusH - y * sinMinusH;
            double rotY = x * sinMinusH + y * cosMinusH;

            rotX = rotX * 1.1;

            double denominator = Math.max(Math.abs(rotY) + Math.abs(rotX) + Math.abs(turn), 1);
            double frontLeftPower = (rotY + rotX + turn) / denominator;
            double backLeftPower = (rotY - rotX + turn) / denominator;
            double frontRightPower = (rotY - rotX - turn) / denominator;
            double backRightPower = (rotY + rotX - turn) / denominator;

            // Scale up low powers to overcome static friction
            double maxAbs = Math.max(
                    Math.max(Math.abs(frontLeftPower), Math.abs(frontRightPower)),
                    Math.max(Math.abs(backLeftPower), Math.abs(backRightPower))
            );
            if (maxAbs > 0 && maxAbs < ControlConstants.MIN_DRIVE_POWER) {
                double powerScale = ControlConstants.MIN_DRIVE_POWER / maxAbs;
                frontLeftPower *= powerScale;
                backLeftPower *= powerScale;
                frontRightPower *= powerScale;
                backRightPower *= powerScale;
            }

            setMotorPowersSmart(frontLeftPower * voltageNorm, backLeftPower * voltageNorm,
                               frontRightPower * voltageNorm, backRightPower * voltageNorm);
            return;
        }

        // If we get here, all movement inputs are zero. Only orientation correction or holding may be necessary.
        // ---- Active Heading Hold ----
        if (activeHeadingHoldEnabled) {
            if (!ControlConstants.ENABLE_ACTIVE_HEADING_CORRECTION) {
                setMotorPowersSmart(0, 0, 0, 0);
                return;
            }
            double customTargetRad = getCustomHeadingRad();
            double headingError = AngleUnit.normalizeRadians(headingRad - customTargetRad);
            double headingErrorDeg = Math.abs(Math.toDegrees(headingError));
            if (headingErrorDeg < ControlConstants.ACTIVE_HEADING_HOLD_DEADBAND_DEG) {
                setMotorPowersSmart(0, 0, 0, 0);
                return;
            } else {
                double dt = currentTimeSeconds - lastActiveHeadingTime;
                double headingErrorDerivative = (headingError - lastActiveHeadingError) / (dt > 0 ? dt : 1e-3);
                double correction = headingError * ControlConstants.ACTIVE_HEADING_P_GAIN
                        + headingErrorDerivative * ControlConstants.ACTIVE_HEADING_D_GAIN;
                if (Math.abs(correction) > 0 && Math.abs(correction) < ControlConstants.ACTIVE_HEADING_MIN_TURN_POWER) {
                    correction = Math.copySign(ControlConstants.ACTIVE_HEADING_MIN_TURN_POWER, correction);
                }
                setMotorPowersSmart(-correction, -correction, correction, correction);
                lastActiveHeadingError = headingError;
                lastActiveHeadingTime = currentTimeSeconds;
                return;
            }
        }

        // ---- Orientation Lock Correction ----
        if (orientationLock && !turnOffOrientationLock) {
            double targetHeadingRad = (currentOrientation == Orientation.CUSTOM) ? getCustomHeadingRad() : currentOrientation.getRadians();
            double headingError = AngleUnit.normalizeRadians(targetHeadingRad - headingRad);
            double headingErrorDeg = Math.abs(Math.toDegrees(headingError));
            if (headingErrorDeg < ControlConstants.AUTO_ORIENTATION_DEADBAND_DEG) {
                setMotorPowersSmart(0, 0, 0, 0);
                return;
            } else {
                double dt = currentTimeSeconds - lastTime;
                double headingErrorDerivative = (headingError - lastHeadingError) / (dt > 0 ? dt : 1e-3);
                double correction = headingError * ControlConstants.AUTO_ORIENTATION_P_GAIN + headingErrorDerivative * ControlConstants.AUTO_ORIENTATION_D_GAIN;
                lastHeadingError = headingError;
                lastTime = currentTimeSeconds;
                setMotorPowersSmart(-correction, -correction, correction, correction);
                return;
            }
        }

        // Default: no power
        setMotorPowersSmart(0, 0, 0, 0);
    }

    public double getTargetHeadingRad() {
        return targetHeadingRad;
    }

    public void lockOrientation(Orientation orientation) {
        this.orientationLock = true;
        this.autoOrientEnabled = false; // Disable auto-orient in favor of manual orientation control
        this.currentOrientation = orientation;
    }

    public void disableOrientationLock() {
        this.orientationLock = false;
        disableAutoOrientation();
    }

    public boolean isOrientationLocked() {
        return orientationLock;
    }

    public Orientation getCurrentOrientation() {
        return currentOrientation;
    }

    /**
     * Enable auto-orientation to maintain a target heading
     *
     * @param targetHeadingRad The target heading in radians
     */
    public void enableAutoOrientation(double targetHeadingRad) {
        this.autoOrientEnabled = true;
        this.orientationLock = false; // Disable orientation lock in favor of auto-orient
        this.targetHeadingRad = targetHeadingRad;
    }

    /**
     * Disable auto-orientation, returning to manual control
     */
    public void disableAutoOrientation() {
        this.autoOrientEnabled = false;
    }

    public void disableActiveHeadingCorrection() {
        this.activeHeadingHoldEnabled = false;
    }

    public boolean isTurnOffOrientationLock() {
        return turnOffOrientationLock;
    }

    /**
     * Check if the robot is currently moving based on gamepad input
     * This helps optimize IMU reading frequency
     *
     * @param gamepad The gamepad to check for movement input
     * @return true if the robot is moving, false if stationary
     */
    public boolean isRobotMoving(Gamepad gamepad) {
        double movementMagnitude = Math.hypot(gamepad.left_stick_x, gamepad.left_stick_y);
        double turnInput = Math.abs(gamepad.right_trigger - gamepad.left_trigger);
        return movementMagnitude > MOVEMENT_THRESHOLD || turnInput > MOVEMENT_THRESHOLD;
    }

    /**
     * Detect if the robot has been impacted by external forces (like another robot)
     * by checking for sudden heading changes that don't match user input
     *
     * @param currentHeading Current heading in radians
     * @param gamepad Gamepad to check if user is turning
     * @return true if an impact/external force is detected
     */
    public boolean detectImpact(double currentHeading, Gamepad gamepad) {
        double headingChange = Math.abs(AngleUnit.normalizeRadians(currentHeading - lastHeadingForImpactDetection));
        boolean userTurning = Math.abs(gamepad.right_trigger - gamepad.left_trigger) > MOVEMENT_THRESHOLD;

        // Update last heading for next comparison
        lastHeadingForImpactDetection = currentHeading;

        // If we detect a large heading change but user isn't turning, likely an impact
        return headingChange > IMPACT_DETECTION_THRESHOLD && !userTurning;
    }
}
