package pedroPathing.tuners_tests;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLStatus;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import pedroPathing.constants.CameraConstants;
import pedroPathing.constants.IntakeConstants;
import pedroPathing.hardware.RobotHardware;
import pedroPathing.util.ButtonEdgeDetector;

/**
 * Vision-Guided Slide Test TeleOp
 * 
 * This TeleOp integrates FatDragon vision detection with automatic slide positioning.
 * It demonstrates how to use vision data to automatically adjust intake slide extension
 * based on detected sample distance.
 * 
 * Features:
 * - Real-time FatDragon vision processing
 * - Automatic slide position calculation based on sample distance
 * - Manual override controls for testing
 * - Comprehensive telemetry for debugging
 * - Safety limits and error handling
 * 
 * Controls:
 * - Gamepad1 A: Auto-position slide based on vision
 * - Gamepad1 B: Reset slide to retracted position
 * - Gamepad1 X: Manual slide extension override
 * - Gamepad1 Y: Manual slide retraction override
 * - Gamepad1 Left Stick: Manual slide control (when not in auto mode)
 * - Gamepad1 Right Stick: Manual turret control
 */
@TeleOp(name = "Vision Slide Test", group = "Test")
public class VisionSlideTestTeleop extends OpMode {

    // Hardware
    private RobotHardware robot;
    private Limelight3A limelight;
    private ButtonEdgeDetector buttonDetector = new ButtonEdgeDetector();
    private ElapsedTime runtime = new ElapsedTime();
    
    // Vision and positioning state
    private boolean autoPositioningEnabled = false;
    private boolean visionDataValid = false;
    private double lastValidDistance = 0.0;
    private double lastValidAngle = 0.0;
    private double lastRobotRelativeX = 0.0;
    private double lastRobotRelativeY = 0.0;
    private double targetSlidePosition = IntakeConstants.SLIDE_MIN;
    private double targetTurretPosition = IntakeConstants.TURRET_MIDDLE;
    
    // Timing for smooth updates
    private ElapsedTime visionUpdateTimer = new ElapsedTime();
    private static final double VISION_UPDATE_INTERVAL = 0.1; // 100ms between vision updates
    
    @Override
    public void init() {
        telemetry.addData("Status", "Initializing Vision Slide Test...");
        telemetry.update();
        
        try {
            // Initialize robot hardware
            robot = new RobotHardware(this);
            robot.init();
            
            // Initialize Limelight
            limelight = hardwareMap.get(Limelight3A.class, "limelight");
            limelight.start();
            limelight.pipelineSwitch(0); // Use FatDragon pipeline
            
            telemetry.addData("Status", "✅ Vision Slide Test Initialized");
            telemetry.addData("Pipeline", "Using Pipeline 0 (FatDragon)");
            telemetry.addData("Ready", "Press PLAY to start");
            telemetry.update();
            
        } catch (Exception e) {
            telemetry.addData("❌ ERROR", "Failed to initialize");
            telemetry.addData("Error Details", e.getMessage());
            telemetry.update();
        }
    }
    
    @Override
    public void start() {
        runtime.reset();
        visionUpdateTimer.reset();
    }
    
    @Override
    public void loop() {
        try {
            // Update button detector
            buttonDetector.update(gamepad1);
            
            // Update vision data periodically
            if (visionUpdateTimer.seconds() > VISION_UPDATE_INTERVAL) {
                updateVisionData();
                visionUpdateTimer.reset();
            }
            
            // Handle controls
            handleControls();
            
            // Update slide positioning
            updateSlidePositioning();
            
            // Display telemetry
            displayTelemetry();
            
        } catch (Exception e) {
            telemetry.addData("❌ RUNTIME ERROR", e.getMessage());
            telemetry.update();
        }
    }
    
    /**
     * Update vision data from FatDragon pipeline
     */
    private void updateVisionData() {
        try {
            LLResult result = limelight.getLatestResult();
            visionDataValid = false;
            
            if (result != null) {
                double[] pythonOutput = result.getPythonOutput();
                if (pythonOutput != null && pythonOutput.length >= 8) {
                    int samplesDetected = (int)pythonOutput[0];
                    
                    if (samplesDetected > 0) {
                        double sampleX = pythonOutput[1];
                        double sampleY = pythonOutput[2];
                        double sampleAngle = pythonOutput[3];
                        int distance = (int)pythonOutput[7];
                        
                        // Calculate improved distance using camera geometry
                        double improvedDistance = calculateImprovedDistance(sampleX, sampleY);
                        
                        // Calculate robot-relative position
                        double[] robotPosition = calculateRobotRelativePosition(sampleX, sampleY, improvedDistance);

                        // Store valid data
                        lastValidDistance = improvedDistance;
                        lastValidAngle = Math.toDegrees(Math.atan2(robotPosition[1], robotPosition[0]));

                        // Store additional debug data
                        lastRobotRelativeX = robotPosition[0];
                        lastRobotRelativeY = robotPosition[1];
                        visionDataValid = true;
                    }
                }
            }
        } catch (Exception e) {
            telemetry.addData("Vision Update Error", e.getMessage());
        }
    }
    
    /**
     * Handle gamepad controls
     */
    private void handleControls() {
        // A button: Enable auto-positioning based on vision
        if (buttonDetector.aPressed(gamepad1)) {
            if (visionDataValid) {
                autoPositioningEnabled = true;
                calculateTargetPositions();
                gamepad1.rumble(200); // Feedback
                telemetry.addData("Control", "✅ Auto-positioning enabled");
            } else {
                telemetry.addData("Control", "❌ No valid vision data");
            }
        }
        
        // B button: Reset to retracted position
        if (buttonDetector.bPressed(gamepad1)) {
            autoPositioningEnabled = false;
            targetSlidePosition = IntakeConstants.SLIDE_MIN;
            targetTurretPosition = IntakeConstants.TURRET_MIDDLE;
            telemetry.addData("Control", "🏠 Reset to home position");
        }
        
        // X button: Manual extension override
        if (buttonDetector.xPressed(gamepad1)) {
            autoPositioningEnabled = false;
            targetSlidePosition = IntakeConstants.SLIDE_MAX;
            telemetry.addData("Control", "⬆️ Manual extension");
        }
        
        // Y button: Manual retraction override
        if (buttonDetector.yPressed(gamepad1)) {
            autoPositioningEnabled = false;
            targetSlidePosition = IntakeConstants.SLIDE_MIN;
            telemetry.addData("Control", "⬇️ Manual retraction");
        }
        
        // Manual slide control when not in auto mode
        if (!autoPositioningEnabled) {
            double slideInput = -gamepad1.left_stick_y;
            if (Math.abs(slideInput) > 0.1) {
                double currentPosition = robot.intake.getIntakeSlidePosition();
                double newPosition = currentPosition + (slideInput * 0.01); // Small increments
                targetSlidePosition = Math.max(IntakeConstants.SLIDE_MAX, 
                                             Math.min(newPosition, IntakeConstants.SLIDE_MIN));
            }
            
            // Manual turret control
            double turretInput = gamepad1.right_stick_x;
            if (Math.abs(turretInput) > 0.1) {
                double currentTurret = robot.intake.getIntakeTurretPosition();
                double newTurret = currentTurret + (turretInput * 0.01);
                targetTurretPosition = Math.max(IntakeConstants.TURRET_RIGHT,
                                              Math.min(newTurret, IntakeConstants.TURRET_LEFT));
            }
        }
    }
    
    /**
     * Calculate target positions based on vision data
     */
    private void calculateTargetPositions() {
        if (!visionDataValid) return;
        
        // Calculate slide position based on distance
        double maxReach = IntakeConstants.AUTO_INTAKE_MAX_REACH;
        double clampedDistance = Math.max(IntakeConstants.AUTO_INTAKE_MIN_DISTANCE,
                                        Math.min(lastValidDistance, maxReach));
        
        // Map distance to slide position (0.0 = full extension, 0.88 = retracted)
        double slideExtension = clampedDistance / maxReach;
        targetSlidePosition = IntakeConstants.SLIDE_MIN - (slideExtension * (IntakeConstants.SLIDE_MIN - IntakeConstants.SLIDE_MAX));
        
        // Calculate turret position based on angle
        double clampedAngle = Math.max(-135.0, Math.min(135.0, lastValidAngle));
        double turretRange = IntakeConstants.TURRET_LEFT - IntakeConstants.TURRET_RIGHT;
        double normalizedAngle = clampedAngle / 135.0; // Normalize to -1 to 1

        // FIXED: Invert the angle calculation for correct turret orientation
        // Positive angle (right side of robot) should move turret toward TURRET_RIGHT (smaller value)
        // Negative angle (left side of robot) should move turret toward TURRET_LEFT (larger value)
        targetTurretPosition = IntakeConstants.TURRET_MIDDLE - (normalizedAngle * turretRange / 2.0);
        
        // Clamp turret position to safe limits
        targetTurretPosition = Math.max(IntakeConstants.TURRET_RIGHT,
                                      Math.min(targetTurretPosition, IntakeConstants.TURRET_LEFT));
    }
    
    /**
     * Update slide positioning
     */
    private void updateSlidePositioning() {
        if (robot.intake != null) {
            robot.intake.setIntakeSlidePosition(targetSlidePosition);
            robot.intake.setIntakeTurretPosition(targetTurretPosition);
        }
    }

    /**
     * Calculate improved distance using camera geometry
     */
    private double calculateImprovedDistance(double pixelX, double pixelY) {
        try {
            // Get vertical angle to target
            double normalizedY = (pixelY - (CameraConstants.CAMERA_HEIGHT_PIXELS / 2.0)) / (CameraConstants.CAMERA_HEIGHT_PIXELS / 2.0);
            double verticalAngle = -normalizedY * (CameraConstants.CAMERA_FOV_VERTICAL / 2.0);

            // Adjust for camera tilt
            double effectiveAngle = Math.toRadians(verticalAngle + CameraConstants.CAMERA_TILT_ANGLE);

            // Calculate distance using trigonometry
            if (Math.abs(effectiveAngle) > 0.01) {
                double distance = (CameraConstants.CAMERA_HEIGHT - CameraConstants.SAMPLE_HEIGHT) / Math.tan(Math.abs(effectiveAngle));

                // Clamp to reasonable limits
                return Math.max(CameraConstants.MIN_DETECTION_DISTANCE,
                       Math.min(distance, CameraConstants.MAX_DETECTION_DISTANCE));
            } else {
                return CameraConstants.DEFAULT_DISTANCE;
            }
        } catch (Exception e) {
            return CameraConstants.DEFAULT_DISTANCE;
        }
    }

    /**
     * Calculate robot-relative position of the sample
     */
    private double[] calculateRobotRelativePosition(double pixelX, double pixelY, double distance) {
        try {
            // Get horizontal angle to target
            double normalizedX = (pixelX - (CameraConstants.CAMERA_WIDTH / 2.0)) / (CameraConstants.CAMERA_WIDTH / 2.0);
            double horizontalAngle = normalizedX * (CameraConstants.CAMERA_FOV_HORIZONTAL / 2.0);

            // Calculate position relative to camera using standard robot coordinates
            double cameraRelativeX = distance * Math.cos(Math.toRadians(horizontalAngle)); // Forward distance
            double cameraRelativeY = distance * Math.sin(Math.toRadians(horizontalAngle)); // Left/right distance

            // Adjust for camera offset from robot center
            double robotRelativeX = cameraRelativeX + CameraConstants.CAMERA_OFFSET_X;
            double robotRelativeY = cameraRelativeY + CameraConstants.CAMERA_OFFSET_Y;

            return new double[]{robotRelativeX, robotRelativeY};
        } catch (Exception e) {
            return new double[]{0.0, 0.0};
        }
    }

    /**
     * Display comprehensive telemetry
     */
    private void displayTelemetry() {
        // Header
        telemetry.addData("=== VISION SLIDE TEST ===", "");
        telemetry.addData("Runtime", String.format("%.1f seconds", runtime.seconds()));
        telemetry.addData("Auto Positioning", autoPositioningEnabled ? "✅ ENABLED" : "❌ DISABLED");

        // Vision status
        telemetry.addData("=== VISION STATUS ===", "");
        telemetry.addData("Vision Data Valid", visionDataValid ? "✅ YES" : "❌ NO");

        if (visionDataValid) {
            telemetry.addData("Sample Distance", String.format("%.1f inches", lastValidDistance));
            telemetry.addData("Sample Angle", String.format("%.1f degrees", lastValidAngle));

            // Add debugging info for turret calculation
            double clampedAngle = Math.max(-135.0, Math.min(135.0, lastValidAngle));
            double normalizedAngle = clampedAngle / 135.0;
            telemetry.addData("Clamped Angle", String.format("%.1f degrees", clampedAngle));
            telemetry.addData("Normalized Angle", String.format("%.3f", normalizedAngle));
            telemetry.addData("Angle Direction", lastValidAngle > 0 ? "RIGHT (positive)" : "LEFT (negative)");
        }

        // Limelight status
        try {
            LLStatus status = limelight.getStatus();
            telemetry.addData("Limelight Status", "✅ Connected");
            telemetry.addData("Pipeline", status.getPipelineIndex());
            telemetry.addData("FPS", String.format("%d", (int)status.getFps()));
            telemetry.addData("Temperature", String.format("%.1f°C", status.getTemp()));
        } catch (Exception e) {
            telemetry.addData("Limelight Status", "❌ Error: " + e.getMessage());
        }

        // Current positions
        telemetry.addData("=== CURRENT POSITIONS ===", "");
        if (robot.intake != null) {
            telemetry.addData("Slide Position", String.format("%.3f", robot.intake.getIntakeSlidePosition()));
            telemetry.addData("Turret Position", String.format("%.3f", robot.intake.getIntakeTurretPosition()));
            telemetry.addData("Wrist Position", String.format("%.3f", robot.intake.getIntakeWristPosition()));
        }

        // Target positions
        telemetry.addData("=== TARGET POSITIONS ===", "");
        telemetry.addData("Target Slide", String.format("%.3f", targetSlidePosition));
        telemetry.addData("Target Turret", String.format("%.3f", targetTurretPosition));

        // Slide extension calculation
        if (visionDataValid) {
            double slideExtension = (IntakeConstants.SLIDE_MIN - targetSlidePosition) / (IntakeConstants.SLIDE_MIN - IntakeConstants.SLIDE_MAX);
            double extensionInches = slideExtension * IntakeConstants.AUTO_INTAKE_MAX_REACH;
            telemetry.addData("Slide Extension", String.format("%.1f%% (%.1f inches)", slideExtension * 100, extensionInches));
        }

        // Controls
        telemetry.addData("=== CONTROLS ===", "");
        telemetry.addData("A", "Auto-position from vision");
        telemetry.addData("B", "Reset to home");
        telemetry.addData("X", "Manual extend");
        telemetry.addData("Y", "Manual retract");
        telemetry.addData("Left Stick Y", "Manual slide control");
        telemetry.addData("Right Stick X", "Manual turret control");

        // Constants for reference
        telemetry.addData("=== CONSTANTS ===", "");
        telemetry.addData("Max Reach", String.format("%.1f inches", IntakeConstants.AUTO_INTAKE_MAX_REACH));
        telemetry.addData("Min Distance", String.format("%.1f inches", IntakeConstants.AUTO_INTAKE_MIN_DISTANCE));
        telemetry.addData("Camera Height", String.format("%.1f inches", CameraConstants.CAMERA_HEIGHT));
        telemetry.addData("Camera Tilt", String.format("%.1f degrees", CameraConstants.CAMERA_TILT_ANGLE));

        telemetry.update();
    }

    @Override
    public void stop() {
        // Reset slide to safe position
        if (robot != null && robot.intake != null) {
            robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MIN);
            robot.intake.setIntakeTurretPosition(IntakeConstants.TURRET_MIDDLE);
        }

        // Stop Limelight
        if (limelight != null) {
            limelight.stop();
        }
    }
}
