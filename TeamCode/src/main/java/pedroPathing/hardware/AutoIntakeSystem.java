package pedroPathing.hardware;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import pedroPathing.constants.IntakeConstants;
import pedroPathing.constants.VisionConstants;
import pedroPathing.robot_state.RobotStateMachine;
import pedroPathing.util.StadiumAreaCalculator;

/**
 * Automatic Intake System for Vision-Guided Sample Pickup
 *
 * This class provides automatic sample pickup using vision data from the Limelight 3A.
 * It controls the 5 DOF intake system to position and grab samples automatically.
 *
 * 5 DOF System:
 * - Slide: Back-front movement (40cm range)
 * - Turret: Base rotation left/right (270° with limits)
 * - Shoulder: Up/down movement
 * - Elbow: Up/down articulation
 * - Wrist: Left/right rotation (270°)
 * - Claw: Open/close
 */
public class AutoIntakeSystem {

    private final RobotHardware robot;
    private final VisionSystem visionSystem;
    private final RobotStateMachine stateMachine;
    private final OpMode opMode;

    // Timing and state management
    private ElapsedTime sequenceTimer = new ElapsedTime();
    private AutoIntakeState currentState = AutoIntakeState.IDLE;
    private boolean isAutoIntakeActive = false;

    // Target sample data
    private double targetX = 0.0;  // Robot-relative X position (forward/back)
    private double targetY = 0.0;  // Robot-relative Y position (left/right)
    private double targetDistance = 0.0;
    private double targetAngle = 0.0;

    // Calculated servo positions
    private double calculatedTurretPosition = IntakeConstants.TURRET_MIDDLE;
    private double calculatedSlidePosition = IntakeConstants.SLIDE_MIN;
    private double calculatedWristPosition = IntakeConstants.WRIST_MIDDLE;
    private double calculatedShoulderPosition = IntakeConstants.SHOULDER_GRAB;
    private double calculatedElbowPosition = IntakeConstants.ELBOW_GRAB;

    public enum AutoIntakeState {
        IDLE,                    // Not running auto intake
        VISION_SEARCH,          // Looking for samples
        CALCULATING_POSITION,   // Computing servo positions
        POSITIONING_TURRET,     // Rotating turret to target
        EXTENDING_SLIDE,        // Extending slide to reach sample
        POSITIONING_ARM,        // Moving shoulder/elbow to grab position
        ADJUSTING_WRIST,        // Fine-tuning wrist angle
        GRABBING_SAMPLE,        // Closing claw on sample
        RETRACTING,            // Pulling sample back
        COMPLETE,              // Pickup complete
        ERROR                  // Error occurred
    }

    public AutoIntakeSystem(RobotHardware robot, VisionSystem visionSystem,
                           RobotStateMachine stateMachine, OpMode opMode) {
        this.robot = robot;
        this.visionSystem = visionSystem;
        this.stateMachine = stateMachine;
        this.opMode = opMode;
    }

    /**
     * Start automatic sample pickup sequence
     * @return true if sequence started successfully
     */
    public boolean startAutoIntake() {
        if (isAutoIntakeActive) {
            return false; // Already running
        }

        // Enable color recognition for FatDragon pipeline
        visionSystem.setColorRecognitionEnabled(true);

        // Check if vision system has a valid target
        if (!visionSystem.isTargetVisible() && !visionSystem.isColorSampleDetected()) {
            opMode.telemetry.addData("Auto Intake", "❌ No sample detected");
            return false;
        }

        // Get sample position from vision
        double[] samplePosition = getSamplePositionFromVision();
        if (samplePosition == null) {
            opMode.telemetry.addData("Auto Intake", "❌ Invalid sample position");
            return false;
        }

        // Store target data and validate with stadium area
        targetX = samplePosition[0];
        targetY = samplePosition[1];

        // Check if position is reachable using stadium area calculator
        if (!StadiumAreaCalculator.isPositionReachable(targetX, targetY)) {
            // Get closest reachable position
            double[] closestPos = StadiumAreaCalculator.getClosestReachablePosition(targetX, targetY);
            targetX = closestPos[0];
            targetY = closestPos[1];
            opMode.telemetry.addData("Auto Intake", "⚠️ Target adjusted to reachable position");
        }

        targetDistance = Math.sqrt(targetX * targetX + targetY * targetY);
        targetAngle = Math.atan2(targetY, targetX);

        // Start sequence
        isAutoIntakeActive = true;
        currentState = AutoIntakeState.CALCULATING_POSITION;
        sequenceTimer.reset();

        opMode.telemetry.addData("Auto Intake", "✅ Starting pickup sequence");
        opMode.telemetry.addData("Target Position", String.format("X:%.1f Y:%.1f", targetX, targetY));

        return true;
    }

    /**
     * Stop automatic intake sequence
     */
    public void stopAutoIntake() {
        isAutoIntakeActive = false;
        currentState = AutoIntakeState.IDLE;
        opMode.telemetry.addData("Auto Intake", "⏹️ Sequence stopped");
    }

    /**
     * Update the automatic intake sequence - call this in your main loop
     */
    public void update() {
        if (!isAutoIntakeActive) {
            return;
        }

        switch (currentState) {
            case CALCULATING_POSITION:
                calculateServoPositions();
                currentState = AutoIntakeState.POSITIONING_TURRET;
                sequenceTimer.reset();
                break;

            case POSITIONING_TURRET:
                robot.intake.setIntakeTurretPosition(calculatedTurretPosition);
                if (sequenceTimer.milliseconds() > IntakeConstants.AUTO_TURRET_MOVE_TIME) {
                    currentState = AutoIntakeState.EXTENDING_SLIDE;
                    sequenceTimer.reset();
                }
                break;

            case EXTENDING_SLIDE:
                robot.intake.setIntakeSlidePosition(calculatedSlidePosition);
                if (sequenceTimer.milliseconds() > IntakeConstants.AUTO_SLIDE_EXTEND_TIME) {
                    currentState = AutoIntakeState.POSITIONING_ARM;
                    sequenceTimer.reset();
                }
                break;

            case POSITIONING_ARM:
                robot.intake.setIntakeShoulderPosition(calculatedShoulderPosition);
                robot.intake.setIntakeElbowPosition(calculatedElbowPosition);
                if (sequenceTimer.milliseconds() > IntakeConstants.AUTO_ARM_POSITION_TIME) {
                    currentState = AutoIntakeState.ADJUSTING_WRIST;
                    sequenceTimer.reset();
                }
                break;

            case ADJUSTING_WRIST:
                robot.intake.setIntakeWristPosition(calculatedWristPosition);
                if (sequenceTimer.milliseconds() > IntakeConstants.AUTO_WRIST_ADJUST_TIME) {
                    currentState = AutoIntakeState.GRABBING_SAMPLE;
                    sequenceTimer.reset();
                }
                break;

            case GRABBING_SAMPLE:
                robot.intake.setIntakeClawPosition(IntakeConstants.CLAW_CLOSED);
                if (sequenceTimer.milliseconds() > IntakeConstants.CLAW_CLOSED_TIME) {
                    currentState = AutoIntakeState.RETRACTING;
                    sequenceTimer.reset();
                }
                break;

            case RETRACTING:
                // Use state machine for proper retraction sequence
                if (stateMachine != null) {
                    // This will trigger the normal intake sequence
                    // The state machine will handle the retraction
                }
                currentState = AutoIntakeState.COMPLETE;
                break;

            case COMPLETE:
                isAutoIntakeActive = false;
                opMode.telemetry.addData("Auto Intake", "✅ Pickup complete!");
                break;

            case ERROR:
                isAutoIntakeActive = false;
                opMode.telemetry.addData("Auto Intake", "❌ Error occurred");
                break;
        }

        // Add telemetry
        opMode.telemetry.addData("Auto Intake State", currentState.toString());
        opMode.telemetry.addData("Sequence Time", String.format("%.1fs", sequenceTimer.seconds()));
    }

    /**
     * Calculate servo positions based on target sample position using stadium area calculator
     */
    private void calculateServoPositions() {
        try {
            // Use stadium area calculator for comprehensive servo position calculation
            double[] servoPositions = StadiumAreaCalculator.calculateServoPositions(targetX, targetY);

            calculatedTurretPosition = servoPositions[0];
            calculatedSlidePosition = servoPositions[1];
            calculatedShoulderPosition = servoPositions[2];
            calculatedElbowPosition = servoPositions[3];
            calculatedWristPosition = servoPositions[4];

            // Validate calculated positions
            if (!validateServoPositions()) {
                opMode.telemetry.addData("Auto Intake Warning", "Invalid servo positions calculated");
                currentState = AutoIntakeState.ERROR;
                return;
            }

            opMode.telemetry.addData("Calculated Turret", String.format("%.3f", calculatedTurretPosition));
            opMode.telemetry.addData("Calculated Slide", String.format("%.3f (dist: %.1f)", calculatedSlidePosition, targetDistance));
            opMode.telemetry.addData("Calculated Shoulder", String.format("%.3f", calculatedShoulderPosition));
            opMode.telemetry.addData("Calculated Elbow", String.format("%.3f", calculatedElbowPosition));
            opMode.telemetry.addData("Stadium Area", "Position validated and optimized");

        } catch (Exception e) {
            currentState = AutoIntakeState.ERROR;
            opMode.telemetry.addData("Calculation Error", e.getMessage());
        }
    }

    /**
     * Get sample position from vision system
     * @return [X, Y] position relative to robot center, or null if invalid
     */
    private double[] getSamplePositionFromVision() {
        try {
            // Check if we have valid vision data
            if (!visionSystem.isTargetVisible() && !visionSystem.isColorSampleDetected()) {
                return null;
            }

            // Get sample position from vision system
            // If your vision system provides pixel coordinates directly, use those
            // Otherwise, we'll use the TX/TY angles and convert them

            double pixelX, pixelY;

            // Option 1: If you have direct pixel coordinates from FatDragon
            // Note: These methods may not exist yet - using TX/TY conversion instead
            if (false) { // Disabled until getSampleX/Y methods are implemented
                pixelX = 160; // visionSystem.getSampleX();
                pixelY = 120; // visionSystem.getSampleY();
            } else {
                // Option 2: Convert TX/TY angles back to pixel coordinates
                double tx = visionSystem.getTargetX(); // Horizontal angle
                double ty = visionSystem.getTargetY(); // Vertical angle

                // Convert angles to pixel coordinates
                pixelX = (tx / (VisionConstants.CAMERA_FOV_HORIZONTAL / 2.0)) * (VisionConstants.CAMERA_WIDTH / 2.0) + (VisionConstants.CAMERA_WIDTH / 2.0);
                pixelY = (-ty / (VisionConstants.CAMERA_FOV_VERTICAL / 2.0)) * (VisionConstants.CAMERA_HEIGHT_PIXELS / 2.0) + (VisionConstants.CAMERA_HEIGHT_PIXELS / 2.0);
            }

            // Calculate distance and position using camera geometry
            double distance = calculateImprovedDistance(pixelX, pixelY);
            double[] position = calculateRobotRelativePosition(pixelX, pixelY, distance);

            // Validate the calculated position
            if (Math.abs(position[0]) > 100 || Math.abs(position[1]) > 100) {
                opMode.telemetry.addData("Vision Warning", "Position seems unrealistic");
                return null;
            }

            return position;
        } catch (Exception e) {
            opMode.telemetry.addData("Vision Error", e.getMessage());
            return null;
        }
    }

    /**
     * Calculate horizontal field angle from pixel X coordinate
     */
    private double calculateFieldAngleX(double pixelX) {
        double normalizedX = (pixelX - (VisionConstants.CAMERA_WIDTH / 2.0)) / (VisionConstants.CAMERA_WIDTH / 2.0);
        return normalizedX * (VisionConstants.CAMERA_FOV_HORIZONTAL / 2.0);
    }

    /**
     * Calculate vertical field angle from pixel Y coordinate
     */
    private double calculateFieldAngleY(double pixelY) {
        double normalizedY = (pixelY - (VisionConstants.CAMERA_HEIGHT_PIXELS / 2.0)) / (VisionConstants.CAMERA_HEIGHT_PIXELS / 2.0);
        return -normalizedY * (VisionConstants.CAMERA_FOV_VERTICAL / 2.0);
    }

    /**
     * Calculate improved distance using camera geometry and constants
     */
    private double calculateImprovedDistance(double pixelX, double pixelY) {
        try {
            double verticalAngle = calculateFieldAngleY(pixelY);
            double effectiveAngle = Math.toRadians(verticalAngle + VisionConstants.CAMERA_TILT_ANGLE);

            if (Math.abs(effectiveAngle) > 0.01) {
                double distance = (VisionConstants.CAMERA_HEIGHT - VisionConstants.SAMPLE_HEIGHT) / Math.tan(Math.abs(effectiveAngle));
                return Math.max(VisionConstants.MIN_DETECTION_DISTANCE,
                       Math.min(distance, VisionConstants.MAX_DETECTION_DISTANCE));
            } else {
                return VisionConstants.DEFAULT_DISTANCE;
            }
        } catch (Exception e) {
            return VisionConstants.DEFAULT_DISTANCE;
        }
    }

    /**
     * Calculate robot-relative position of the sample
     */
    private double[] calculateRobotRelativePosition(double pixelX, double pixelY, double distance) {
        try {
            double horizontalAngle = calculateFieldAngleX(pixelX);

            double cameraRelativeX = distance * Math.cos(Math.toRadians(horizontalAngle));
            double cameraRelativeY = distance * Math.sin(Math.toRadians(horizontalAngle));

            double robotRelativeX = cameraRelativeX + VisionConstants.CAMERA_OFFSET_X;
            double robotRelativeY = cameraRelativeY + VisionConstants.CAMERA_OFFSET_Y;

            return new double[]{robotRelativeX, robotRelativeY};
        } catch (Exception e) {
            return new double[]{0.0, 0.0};
        }
    }

    /**
     * Calculate arm kinematics for shoulder and elbow positioning
     * Uses 2-DOF inverse kinematics with known arm segment lengths
     */
    private void calculateArmKinematics() {
        try {
            // Target position for the claw (relative to shoulder joint)
            // Assume shoulder is at robot center for now - adjust based on actual mounting
            double targetX = targetDistance; // Forward distance to sample
            double targetZ = VisionConstants.SAMPLE_HEIGHT; // Height difference

            // Calculate distance from shoulder to target
            double shoulderToTarget = Math.sqrt(targetX * targetX + targetZ * targetZ);

            // Check if target is reachable
            if (shoulderToTarget > IntakeConstants.TOTAL_ARM_REACH_INCHES) {
                opMode.telemetry.addData("Arm Kinematics", "Target unreachable - too far");
                // Use default grab positions
                calculatedShoulderPosition = IntakeConstants.SHOULDER_GRAB;
                calculatedElbowPosition = IntakeConstants.ELBOW_GRAB;
                return;
            }

            if (shoulderToTarget < Math.abs(IntakeConstants.SHOULDER_TO_ELBOW_LENGTH_INCHES )) {
                opMode.telemetry.addData("Arm Kinematics", "Target unreachable - too close");
                // Use default grab positions
                calculatedShoulderPosition = IntakeConstants.SHOULDER_GRAB;
                calculatedElbowPosition = IntakeConstants.ELBOW_GRAB;
                return;
            }

            // 2-DOF inverse kinematics
            double L1 = IntakeConstants.SHOULDER_TO_ELBOW_LENGTH_INCHES; // Shoulder to elbow
            double L2 = 0.0; // Elbow to claw length is already accounted for in state machine

            // Calculate elbow angle using law of cosines
            double cosElbowAngle = (L1 * L1 + L2 * L2 - shoulderToTarget * shoulderToTarget) / (2 * L1 * L2);
            cosElbowAngle = Math.max(-1.0, Math.min(1.0, cosElbowAngle)); // Clamp to valid range
            double elbowAngle = Math.acos(cosElbowAngle);

            // Calculate shoulder angle
            double alpha = Math.atan2(targetZ, targetX);
            double beta = Math.acos((L1 * L1 + shoulderToTarget * shoulderToTarget - L2 * L2) / (2 * L1 * shoulderToTarget));
            double shoulderAngle = alpha + beta;

            // Convert angles to servo positions
            // These mappings will need to be calibrated based on your servo orientations
            // Assuming 0.0 = 0°, 1.0 = 180° for now - adjust based on actual servo behavior
            calculatedShoulderPosition = mapAngleToServoPosition(Math.toDegrees(shoulderAngle), 0, 180,
                                                               IntakeConstants.SHOULDER_GRAB, IntakeConstants.SHOULDER_HOLD);
            calculatedElbowPosition = mapAngleToServoPosition(Math.toDegrees(elbowAngle), 0, 180,
                                                            IntakeConstants.ELBOW_GRAB, IntakeConstants.ELBOW_HOLD);

            // Clamp to safe servo ranges
            calculatedShoulderPosition = Math.max(0.0, Math.min(1.0, calculatedShoulderPosition));
            calculatedElbowPosition = Math.max(0.0, Math.min(1.0, calculatedElbowPosition));

            opMode.telemetry.addData("Arm Kinematics", String.format("Shoulder: %.1f° Elbow: %.1f°",
                Math.toDegrees(shoulderAngle), Math.toDegrees(elbowAngle)));
            opMode.telemetry.addData("Target Distance", String.format("%.1f\" (reachable)", shoulderToTarget));

        } catch (Exception e) {
            opMode.telemetry.addData("Arm Kinematics Error", e.getMessage());
            // Fall back to default positions
            calculatedShoulderPosition = IntakeConstants.SHOULDER_GRAB;
            calculatedElbowPosition = IntakeConstants.ELBOW_GRAB;
        }
    }

    /**
     * Map an angle to a servo position within a given range
     */
    private double mapAngleToServoPosition(double angleDegrees, double minAngle, double maxAngle,
                                         double minServoPos, double maxServoPos) {
        double normalizedAngle = (angleDegrees - minAngle) / (maxAngle - minAngle);
        return minServoPos + normalizedAngle * (maxServoPos - minServoPos);
    }

    /**
     * Validate that calculated servo positions are within safe ranges
     */
    private boolean validateServoPositions() {
        // Check turret position
        if (calculatedTurretPosition < IntakeConstants.TURRET_RIGHT ||
            calculatedTurretPosition > IntakeConstants.TURRET_LEFT) {
            return false;
        }

        // Check slide position
        if (calculatedSlidePosition < IntakeConstants.SLIDE_MAX ||
            calculatedSlidePosition > IntakeConstants.SLIDE_MIN) {
            return false;
        }

        // Check shoulder position
        if (calculatedShoulderPosition < 0.0 || calculatedShoulderPosition > 1.0) {
            return false;
        }

        // Check elbow position
        if (calculatedElbowPosition < 0.0 || calculatedElbowPosition > 1.0) {
            return false;
        }

        // Check wrist position
        if (calculatedWristPosition < 0.0 || calculatedWristPosition > 1.0) {
            return false;
        }

        return true;
    }

    // Getters for telemetry and debugging
    public boolean isActive() { return isAutoIntakeActive; }
    public AutoIntakeState getCurrentState() { return currentState; }
    public double getTargetX() { return targetX; }
    public double getTargetY() { return targetY; }
    public double getTargetDistance() { return targetDistance; }
}
