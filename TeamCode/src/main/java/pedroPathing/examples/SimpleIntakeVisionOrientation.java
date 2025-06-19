package pedroPathing.examples;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import pedroPathing.BaseTeleop25152;
import pedroPathing.constants.IntakeConstants;
import pedroPathing.constants.VisionConstants;
import pedroPathing.robot_state.RobotMode;

/**
 * Simple Intake Vision Orientation
 * 
 * This teleop automatically orients the intake slide and turret toward detected samples
 * using the robot relative position coordinates from the Limelight 3A.
 * 
 * Features:
 * - Automatic slide positioning based on sample distance
 * - Automatic turret positioning based on sample angle
 * - Real-time vision feedback and servo positioning
 * - Manual override controls
 * - Simple, reliable operation
 * 
 * Controls:
 * - A: Enable/Disable auto-orientation
 * - B: Manual turret center
 * - X: Manual slide retract
 * - Y: Manual slide extend
 * - Left Stick X: Manual turret control (when auto disabled)
 * - Right Stick Y: Manual slide control (when auto disabled)
 * 
 * @author Baron Henderson - 20077 The Indubitables
 * @version 1.0, 12/30/2024
 */
@TeleOp(name = "Simple Intake Vision Orientation", group = "Examples")
public class SimpleIntakeVisionOrientation extends BaseTeleop25152 {

    // Vision system
    private Limelight3A limelight;
    private ElapsedTime visionTimer = new ElapsedTime();
    
    // Auto-orientation state
    private boolean autoOrientationEnabled = false;
    private boolean sampleDetected = false;
    private double targetForward = 0.0;
    private double targetLeft = 0.0;
    private double targetDistance = 0.0;
    private double targetAngle = 0.0;
    
    // Calculated servo positions
    private double calculatedSlidePosition = IntakeConstants.SLIDE_MIN;
    private double calculatedTurretPosition = IntakeConstants.TURRET_MIDDLE;
    
    // Update timing
    private static final long VISION_UPDATE_INTERVAL_MS = 100; // 10 Hz
    private long lastVisionUpdate = 0;

    @Override
    protected RobotMode getRobotMode() {
        return RobotMode.SAMPLE;
    }

    @Override
    protected BaseTeleop25152.TeamColor getTeamColor() {
        return BaseTeleop25152.TeamColor.BLUE;
    }

    @Override
    public void init() {
        super.init();
        
        try {
            // Initialize Limelight 3A
            limelight = hardwareMap.get(Limelight3A.class, "limelight");
            limelight.start();
            limelight.pipelineSwitch(0); // Use Pipeline 0 (FatDragon)
            
            telemetry.addData("Status", "✅ Vision system initialized");
        } catch (Exception e) {
            telemetry.addData("❌ ERROR", "Failed to initialize Limelight 3A");
            telemetry.addData("Details", e.getMessage());
            limelight = null;
        }
        
        telemetry.addData("Auto Orientation", autoOrientationEnabled ? "ENABLED" : "DISABLED");
        telemetry.update();
    }

    @Override
    public void loop() {
        // Call parent loop for standard functionality
        super.loop();
        
        // Update vision data
        updateVisionData();
        
        // Handle controls
        handleControls();
        
        // Update intake positioning
        updateIntakePositioning();
        
        // Display telemetry
        displayVisionTelemetry();
    }

    /**
     * Update vision data from Limelight 3A
     */
    private void updateVisionData() {
        if (limelight == null) return;
        
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastVisionUpdate < VISION_UPDATE_INTERVAL_MS) {
            return; // Limit update rate
        }
        lastVisionUpdate = currentTime;
        
        try {
            LLResult result = limelight.getLatestResult();
            if (result != null) {
                double[] pythonOutput = result.getPythonOutput();
                
                if (pythonOutput != null && pythonOutput.length >= 8) {
                    int samplesDetected = (int) pythonOutput[0];
                    
                    if (samplesDetected > 0) {
                        // Extract pixel coordinates from Python script
                        double pixelX = pythonOutput[1];
                        double pixelY = pythonOutput[2];
                        
                        // Calculate robot relative position (same as SimpleCameraTest)
                        double distance = calculateDistance(pixelX, pixelY);
                        double[] robotPosition = calculateRobotPosition(pixelX, pixelY, distance);
                        
                        // Store target data
                        targetForward = robotPosition[0];
                        targetLeft = robotPosition[1];
                        targetDistance = Math.sqrt(targetForward * targetForward + targetLeft * targetLeft);
                        targetAngle = Math.toDegrees(Math.atan2(targetLeft, targetForward));
                        
                        sampleDetected = true;
                    } else {
                        sampleDetected = false;
                    }
                } else {
                    sampleDetected = false;
                }
            }
        } catch (Exception e) {
            sampleDetected = false;
            telemetry.addData("Vision Error", e.getMessage());
        }
    }

    /**
     * Handle gamepad controls
     */
    private void handleControls() {
        // Toggle auto-orientation
        if (gamepad1.a && !gamepad1.start) {
            autoOrientationEnabled = !autoOrientationEnabled;
            telemetry.addData("Auto Orientation", autoOrientationEnabled ? "ENABLED" : "DISABLED");
        }
        
        // Manual controls (only when auto-orientation is disabled)
        if (!autoOrientationEnabled) {
            // Manual turret control with left stick X
            if (Math.abs(gamepad1.left_stick_x) > 0.1) {
                double turretInput = gamepad1.left_stick_x;
                double turretPosition = IntakeConstants.TURRET_MIDDLE + 
                    turretInput * (IntakeConstants.TURRET_LEFT - IntakeConstants.TURRET_RIGHT) / 2.0;
                turretPosition = Math.max(IntakeConstants.TURRET_RIGHT, 
                                        Math.min(turretPosition, IntakeConstants.TURRET_LEFT));
                getRobot().intake.setIntakeTurretPosition(turretPosition);
            }
            
            // Manual slide control with right stick Y
            if (Math.abs(gamepad1.right_stick_y) > 0.1) {
                double slideInput = -gamepad1.right_stick_y; // Invert for intuitive control
                double slidePosition = IntakeConstants.SLIDE_MIN + 
                    (slideInput + 1.0) / 2.0 * (IntakeConstants.SLIDE_MAX - IntakeConstants.SLIDE_MIN);
                slidePosition = Math.max(IntakeConstants.SLIDE_MAX, 
                                       Math.min(slidePosition, IntakeConstants.SLIDE_MIN));
                getRobot().intake.setIntakeSlidePosition(slidePosition);
            }
        }
        
        // Quick manual controls (work regardless of auto-orientation state)
        if (gamepad1.b) {
            // Center turret
            getRobot().intake.setIntakeTurretPosition(IntakeConstants.TURRET_MIDDLE);
        }
        
        if (gamepad1.x) {
            // Retract slide
            getRobot().intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MIN);
        }
        
        if (gamepad1.y) {
            // Extend slide
            getRobot().intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MAX);
        }
    }

    /**
     * Update intake positioning based on vision data
     */
    private void updateIntakePositioning() {
        if (!autoOrientationEnabled || !sampleDetected) {
            return;
        }
        
        // Calculate servo positions based on target
        calculateServoPositions();
        
        // Apply calculated positions
        getRobot().intake.setIntakeTurretPosition(calculatedTurretPosition);
        getRobot().intake.setIntakeSlidePosition(calculatedSlidePosition);
    }

    /**
     * Calculate servo positions based on target sample position using asymmetric angle limits
     */
    private void calculateServoPositions() {
        // Calculate turret position based on angle with asymmetric limits
        // Left side (positive angles) has different limit than right side (negative angles)
        double clampedAngle;
        if (targetAngle >= 0) {
            // Left side - use left max angle
            clampedAngle = Math.min(targetAngle, IntakeConstants.AUTO_INTAKE_TURRET_LEFT_MAX_ANGLE);
        } else {
            // Right side - use right max angle
            clampedAngle = Math.max(targetAngle, -IntakeConstants.AUTO_INTAKE_TURRET_RIGHT_MAX_ANGLE);
        }

        // Map angle to turret servo position using asymmetric scaling
        double turretRange = IntakeConstants.TURRET_LEFT - IntakeConstants.TURRET_RIGHT;
        double normalizedAngle;

        if (clampedAngle >= 0) {
            // Left side: scale by left max angle
            normalizedAngle = clampedAngle / IntakeConstants.AUTO_INTAKE_TURRET_LEFT_MAX_ANGLE;
        } else {
            // Right side: scale by right max angle
            normalizedAngle = clampedAngle / IntakeConstants.AUTO_INTAKE_TURRET_RIGHT_MAX_ANGLE;
        }

        calculatedTurretPosition = IntakeConstants.TURRET_MIDDLE + (normalizedAngle * turretRange / 2.0);

        // Clamp turret position to safe limits
        calculatedTurretPosition = Math.max(IntakeConstants.TURRET_RIGHT,
                                          Math.min(calculatedTurretPosition, IntakeConstants.TURRET_LEFT));
        
        // Calculate slide position based on distance
        double clampedDistance = Math.max(IntakeConstants.AUTO_INTAKE_MIN_DISTANCE,
                                        Math.min(targetDistance, IntakeConstants.AUTO_INTAKE_MAX_SLIDE_REACH));
        
        // Map distance to slide position (closer = more extended)
        double slideExtension = clampedDistance / IntakeConstants.AUTO_INTAKE_MAX_SLIDE_REACH;
        calculatedSlidePosition = IntakeConstants.SLIDE_MIN - 
            (slideExtension * (IntakeConstants.SLIDE_MIN - IntakeConstants.SLIDE_MAX));
    }

    /**
     * Display comprehensive vision and positioning telemetry
     */
    private void displayVisionTelemetry() {
        telemetry.addLine("=== SIMPLE INTAKE VISION ORIENTATION ===");
        telemetry.addLine();
        
        // Auto-orientation status
        telemetry.addData("Auto Orientation", autoOrientationEnabled ? "✅ ENABLED" : "❌ DISABLED");
        telemetry.addData("Sample Detected", sampleDetected ? "✅ YES" : "❌ NO");
        
        if (sampleDetected) {
            // Target information
            telemetry.addLine();
            telemetry.addData("Target Position", String.format("Forward:%.1f Left:%.1f", targetForward, targetLeft));
            telemetry.addData("Target Distance", String.format("%.1f inches", targetDistance));
            telemetry.addData("Target Angle", String.format("%.1f degrees", targetAngle));
            
            // Calculated servo positions
            if (autoOrientationEnabled) {
                telemetry.addLine();
                telemetry.addData("Calculated Turret", String.format("%.3f", calculatedTurretPosition));
                telemetry.addData("Calculated Slide", String.format("%.3f", calculatedSlidePosition));
            }
        }
        
        // Current servo positions
        telemetry.addLine();
        telemetry.addData("Current Turret", String.format("%.3f", getRobot().intake.getIntakeTurretPosition()));
        telemetry.addData("Current Slide", String.format("%.3f", getRobot().intake.getIntakeSlidePosition()));
        
        // Controls
        telemetry.addLine();
        telemetry.addLine("=== CONTROLS ===");
        telemetry.addLine("A: Toggle Auto-Orientation");
        telemetry.addLine("B: Center Turret");
        telemetry.addLine("X: Retract Slide");
        telemetry.addLine("Y: Extend Slide");
        if (!autoOrientationEnabled) {
            telemetry.addLine("Left Stick X: Manual Turret");
            telemetry.addLine("Right Stick Y: Manual Slide");
        }
    }

    // Vision calculation methods (same as SimpleCameraTest)
    private double calculateDistance(double pixelX, double pixelY) {
        try {
            double verticalAngle = calculateVerticalAngle(pixelY);
            double effectiveAngleRadians = Math.toRadians(verticalAngle + VisionConstants.CAMERA_TILT_ANGLE);
            double heightDifference = VisionConstants.CAMERA_HEIGHT - VisionConstants.SAMPLE_HEIGHT;

            if (Math.abs(effectiveAngleRadians) > 0.01 && Math.tan(effectiveAngleRadians) > 0.01) {
                double distance = heightDifference / Math.tan(effectiveAngleRadians);
                distance = Math.abs(distance);
                return Math.max(VisionConstants.MIN_DETECTION_DISTANCE,
                       Math.min(distance, VisionConstants.MAX_DETECTION_DISTANCE));
            } else {
                return VisionConstants.DEFAULT_DISTANCE;
            }
        } catch (Exception e) {
            return VisionConstants.DEFAULT_DISTANCE;
        }
    }

    private double[] calculateRobotPosition(double pixelX, double pixelY, double distance) {
        try {
            double horizontalAngle = calculateHorizontalAngle(pixelX);
            double cameraRelativeForward = distance * Math.cos(Math.toRadians(horizontalAngle));
            double cameraRelativeLeft = distance * Math.sin(Math.toRadians(horizontalAngle));
            double robotRelativeForward = cameraRelativeForward + VisionConstants.CAMERA_OFFSET_X;
            double robotRelativeLeft = cameraRelativeLeft + VisionConstants.CAMERA_OFFSET_Y;
            return new double[]{robotRelativeForward, robotRelativeLeft};
        } catch (Exception e) {
            return new double[]{0.0, 0.0};
        }
    }

    private double calculateHorizontalAngle(double pixelX) {
        double actualWidth = VisionConstants.CAMERA_WIDTH;
        double normalizedX = -(pixelX - (actualWidth / 2.0)) / (actualWidth / 2.0);
        return normalizedX * (VisionConstants.CAMERA_FOV_HORIZONTAL / 2.0);
    }

    private double calculateVerticalAngle(double pixelY) {
        double actualHeight = VisionConstants.CAMERA_HEIGHT_PIXELS;
        double normalizedY = (pixelY - (actualHeight / 2.0)) / (actualHeight / 2.0);
        return normalizedY * (VisionConstants.CAMERA_FOV_VERTICAL / 2.0);
    }
}
