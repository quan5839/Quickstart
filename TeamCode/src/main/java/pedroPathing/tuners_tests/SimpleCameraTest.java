package pedroPathing.tuners_tests;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLStatus;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import pedroPathing.constants.VisionConstants;

/**
 * Simple Camera Test for FatDragon Yellow Sample Detection
 * 
 * This OpMode provides a clean, simple test of the camera with careful distance calculations
 * using the camera constants. No complex features - just reliable detection and positioning.
 * 
 * Features:
 * - Raw Python output display
 * - Careful distance calculation using trigonometry
 * - Robot-relative position calculation
 * - Simple, clean telemetry output
 * 
 * Instructions:
 * 1. Ensure FatDragon Python script is uploaded to Pipeline 0
 * 2. Run this OpMode to test yellow sample detection
 * 3. Place yellow samples in front of camera to test
 * 
 * @author Baron Henderson - 20077 The Indubitables
 */
@TeleOp(name = "Simple Camera Test", group = "Test")
public class SimpleCameraTest extends LinearOpMode {

    private Limelight3A limelight;
    private ElapsedTime runtime = new ElapsedTime();

    @Override
    public void runOpMode() throws InterruptedException {
        telemetry.addData("Status", "Initializing Simple Camera Test...");
        telemetry.update();

        try {
            // Initialize Limelight 3A
            limelight = hardwareMap.get(Limelight3A.class, "limelight");
            
            // Set telemetry update rate
            telemetry.setMsTransmissionInterval(100);
            
            // Start Limelight and use Pipeline 0 (FatDragon)
            limelight.start();
            limelight.pipelineSwitch(0);

            telemetry.addData("Status", "✅ Camera initialized successfully!");
            telemetry.addData("Pipeline", "Using Pipeline 0 (FatDragon Yellow Detection)");
            telemetry.addData("Info", "Press PLAY to start camera test");
            telemetry.update();
            
        } catch (Exception e) {
            telemetry.addData("❌ ERROR", "Failed to initialize camera");
            telemetry.addData("Details", e.getMessage());
            telemetry.update();
            
            waitForStart();
            return;
        }

        waitForStart();
        runtime.reset();

        while (opModeIsActive()) {
            try {
                // Get camera status and results
                LLStatus status = limelight.getStatus();
                LLResult result = limelight.getLatestResult();

                // Display basic camera info
                telemetry.addData("=== CAMERA STATUS ===", "");
                telemetry.addData("Runtime", String.format("%.1f seconds", runtime.seconds()));
                telemetry.addData("FPS", String.format("%.0f", status.getFps()));
                telemetry.addData("Temperature", String.format("%.1f°C", status.getTemp()));
                telemetry.addData("Pipeline", String.format("%d (%s)", 
                    status.getPipelineIndex(), status.getPipelineType()));

                telemetry.addData("=== SAMPLE DETECTION ===", "");

                if (result != null) {
                    // Get Python output from FatDragon script
                    double[] pythonOutput = result.getPythonOutput();
                    
                    if (pythonOutput != null && pythonOutput.length >= 8) {
                        int samplesDetected = (int) pythonOutput[0];
                        
                        if (samplesDetected > 0) {
                            // Extract raw data from Python script
                            double pixelX = pythonOutput[1];  // Raw pixel X coordinate
                            double pixelY = pythonOutput[2];  // Raw pixel Y coordinate
                            double angle = pythonOutput[3];   // Sample angle in degrees
                            int totalContours = (int) pythonOutput[4]; // Total contours found
                            
                            telemetry.addData("🎯 YELLOW SAMPLE DETECTED", "");
                            telemetry.addData("Raw Pixel Position", String.format("X:%.0f Y:%.0f", pixelX, pixelY));
                            telemetry.addData("Sample Angle", String.format("%.1f°", angle));
                            telemetry.addData("Total Contours", totalContours);
                            
                            // Calculate field angles for debugging
                            double[] fieldAngles = calculateFieldAngles(pixelX, pixelY);
                            telemetry.addData("Field Angles",
                                String.format("Horizontal:%.1f° Vertical:%.1f°", fieldAngles[0], fieldAngles[1]));

                            // Calculate distance using camera constants
                            double distance = calculateDistance(pixelX, pixelY);

                            // Show intermediate calculations for verification
                            double verticalAngle = fieldAngles[1];
                            double effectiveAngle = verticalAngle + VisionConstants.CAMERA_TILT_ANGLE;
                            double heightDiff = VisionConstants.CAMERA_HEIGHT - VisionConstants.SAMPLE_HEIGHT;
                            telemetry.addData("Distance Calc Debug",
                                String.format("VAngle:%.1f° Effective:%.1f° HeightDiff:%.1f",
                                verticalAngle, effectiveAngle, heightDiff));

                            // Calculate robot-relative position
                            double[] robotPosition = calculateRobotPosition(pixelX, pixelY, distance);
                            boolean isReachable = isWithinIntakeReach(robotPosition[0], robotPosition[1]);
                            String reachStatus = isReachable ? "✅ REACHABLE" : "❌ OUT OF REACH";
                            telemetry.addData("Robot Relative Position",
                                String.format("Forward:%.1f Left:%.1f (%s)", robotPosition[0], robotPosition[1], reachStatus));

                            // Show camera-relative position for debugging
                            double horizontalAngle = fieldAngles[0];
                            double cameraForward = distance * Math.cos(Math.toRadians(horizontalAngle));
                            double cameraLeft = distance * Math.sin(Math.toRadians(horizontalAngle));
                            telemetry.addData("Camera Relative Position",
                                String.format("Forward:%.1f Left:%.1f", cameraForward, cameraLeft));
                            
                        } else {
                            telemetry.addData("Sample Status", "❌ No yellow samples detected");
                        }
                        
                        // Always show raw Python output for debugging
                        telemetry.addData("Raw Python Output", 
                            String.format("[%.0f,%.0f,%.0f,%.1f,%d,%d,%d,%d]",
                                pythonOutput[0], pythonOutput[1], pythonOutput[2], pythonOutput[3],
                                (int)pythonOutput[4], (int)pythonOutput[5], (int)pythonOutput[6], (int)pythonOutput[7]));
                        
                    } else {
                        telemetry.addData("Python Script", "❌ No Python output - check script upload");
                    }
                    
                } else {
                    telemetry.addData("Camera Data", "❌ No camera data available");
                }

                // Display camera constants for reference
                telemetry.addData("=== CAMERA CONSTANTS ===", "");
                telemetry.addData("Camera Height", String.format("%.1f inches", VisionConstants.CAMERA_HEIGHT));
                telemetry.addData("Camera Tilt", String.format("%.1f degrees", VisionConstants.CAMERA_TILT_ANGLE));
                telemetry.addData("Camera Offset", String.format("Forward:%.1f Left:%.1f",
                    VisionConstants.CAMERA_OFFSET_X, VisionConstants.CAMERA_OFFSET_Y));
                telemetry.addData("Sample Height", String.format("%.1f inches", VisionConstants.SAMPLE_HEIGHT));

                // Add validation warnings and resolution info
                telemetry.addData("=== VALIDATION NOTES ===", "");
                telemetry.addData("Resolution", String.format("%dx%d",
                    VisionConstants.CAMERA_WIDTH, VisionConstants.CAMERA_HEIGHT_PIXELS));
                telemetry.addData("FOV", String.format("H:%.1f° V:%.1f°",
                    VisionConstants.CAMERA_FOV_HORIZONTAL, VisionConstants.CAMERA_FOV_VERTICAL));

                if (VisionConstants.CAMERA_HEIGHT < 5.0 || VisionConstants.CAMERA_HEIGHT > 20.0) {
                    telemetry.addData("⚠️ Camera Height", "Unusual value - verify measurement");
                }
                if (VisionConstants.CAMERA_TILT_ANGLE < 10.0 || VisionConstants.CAMERA_TILT_ANGLE > 45.0) {
                    telemetry.addData("⚠️ Camera Tilt", "Unusual value - verify angle");
                }

                // Resolution validation
                if (VisionConstants.CAMERA_WIDTH == 640 && VisionConstants.CAMERA_HEIGHT_PIXELS == 480) {
                    telemetry.addData("📹 Resolution", "Using 640x480 (high res)");
                } else if (VisionConstants.CAMERA_WIDTH == 320 && VisionConstants.CAMERA_HEIGHT_PIXELS == 240) {
                    telemetry.addData("📹 Resolution", "Using 320x240 (standard)");
                } else {
                    telemetry.addData("⚠️ Resolution", "Non-standard resolution - verify settings");
                }

                // Show intake reach area info
                double slideReachInches = VisionConstants.INTAKE_SLIDE_MAX_EXTENSION_IN;
                double shoulderReachInches = VisionConstants.INTAKE_SHOULDER_REACH_IN;
                telemetry.addData("=== INTAKE REACH AREA ===", "");
                telemetry.addData("Intake Position", String.format("Forward:%.1f Left:%.1f from robot center",
                    VisionConstants.INTAKE_BASE_OFFSET_X, VisionConstants.INTAKE_BASE_OFFSET_Y));
                telemetry.addData("Slide Reach", String.format("%.1f inches forward", slideReachInches));
                telemetry.addData("Shoulder Reach", String.format("±%.1f inches left/right", shoulderReachInches));

                if (VisionConstants.ENABLE_INSIDE_AREA_REACH) {
                    telemetry.addData("Mode", "FULL AREA (less accurate, all positions)");
                } else {
                    telemetry.addData("Mode", "BOUNDARY ONLY (more accurate, perimeter lines)");
                }

                telemetry.addData("💡 Tip", "Place sample at known distance to verify calculations");

            } catch (Exception e) {
                telemetry.addData("❌ Runtime Error", e.getMessage());
            }
            
            telemetry.update();
        }
        
        // Clean shutdown
        if (limelight != null) {
            limelight.stop();
        }
    }

    /**
     * Calculate distance to sample using camera geometry and trigonometry
     *
     * @param pixelX X coordinate in pixels (from Python script)
     * @param pixelY Y coordinate in pixels (from Python script)
     * @return Distance in inches
     */
    private double calculateDistance(double pixelX, double pixelY) {
        try {
            // Convert pixel Y to vertical angle relative to camera center
            double verticalAngle = calculateVerticalAngle(pixelY);

            // CRITICAL: Camera tilt angle adjustment
            // Camera is tilted DOWN by CAMERA_TILT_ANGLE degrees
            // When looking at a sample on the ground, the effective angle is:
            // effective_angle = vertical_angle_from_center + camera_tilt_angle
            double effectiveAngleRadians = Math.toRadians(verticalAngle + VisionConstants.CAMERA_TILT_ANGLE);

            // Calculate distance using trigonometry
            // For a camera looking down at an angle:
            // distance = height_difference / tan(effective_angle)
            double heightDifference = VisionConstants.CAMERA_HEIGHT - VisionConstants.SAMPLE_HEIGHT;

            // Validate angle to avoid division by zero or invalid calculations
            if (Math.abs(effectiveAngleRadians) > 0.01 && Math.tan(effectiveAngleRadians) > 0.01) {
                double distance = heightDifference / Math.tan(effectiveAngleRadians);

                // Clamp to reasonable limits and ensure positive distance
                distance = Math.abs(distance); // Ensure positive
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
     *
     * @param pixelX X coordinate in pixels
     * @param pixelY Y coordinate in pixels
     * @param distance Distance to sample in inches
     * @return Array with [forward/backward, left/right] position relative to robot center
     */
    private double[] calculateRobotPosition(double pixelX, double pixelY, double distance) {
        try {
            // Get horizontal angle to target (positive = left, negative = right)
            double horizontalAngle = calculateHorizontalAngle(pixelX);

            // Calculate position relative to camera center
            // Using standard trigonometry:
            // - Forward distance = distance * cos(angle)
            // - Left/right distance = distance * sin(angle)
            double cameraRelativeForward = distance * Math.cos(Math.toRadians(horizontalAngle));
            double cameraRelativeLeft = distance * Math.sin(Math.toRadians(horizontalAngle));

            // Transform from camera coordinates to robot coordinates
            // Camera is mounted at offset (CAMERA_OFFSET_X, CAMERA_OFFSET_Y) from robot center
            // Robot coordinate system: +X = forward, +Y = left
            double robotRelativeForward = cameraRelativeForward + VisionConstants.CAMERA_OFFSET_X;
            double robotRelativeLeft = cameraRelativeLeft + VisionConstants.CAMERA_OFFSET_Y;

            return new double[]{robotRelativeForward, robotRelativeLeft};

        } catch (Exception e) {
            return new double[]{0.0, 0.0};
        }
    }

    /**
     * Calculate field angles from pixel coordinates
     * 
     * @param pixelX X coordinate in pixels
     * @param pixelY Y coordinate in pixels
     * @return Array with [horizontal_angle, vertical_angle] in degrees
     */
    private double[] calculateFieldAngles(double pixelX, double pixelY) {
        double horizontalAngle = calculateHorizontalAngle(pixelX);
        double verticalAngle = calculateVerticalAngle(pixelY);
        return new double[]{horizontalAngle, verticalAngle};
    }

    /**
     * Convert pixel X coordinate to horizontal angle
     *
     * @param pixelX X coordinate in pixels (from Python script)
     * @return Horizontal angle in degrees (positive = left, negative = right)
     */
    private double calculateHorizontalAngle(double pixelX) {
        // Using confirmed 320x240 resolution from VisionConstants
        double actualWidth = VisionConstants.CAMERA_WIDTH; // 320 pixels

        // Convert pixel to normalized coordinate (-1 to 1)
        // Center pixel should give 0 degrees
        // Invert normalizedX to fix X mirroring
        double normalizedX = -(pixelX - (actualWidth / 2.0)) / (actualWidth / 2.0);

        // Convert to angle using camera FOV
        // Positive angle = left side of image, Negative angle = right side
        return normalizedX * (VisionConstants.CAMERA_FOV_HORIZONTAL / 2.0);
    }

    /**
     * Convert pixel Y coordinate to vertical angle
     *
     * @param pixelY Y coordinate in pixels (from Python script)
     * @return Vertical angle in degrees (positive = up, negative = down)
     */
    private double calculateVerticalAngle(double pixelY) {
        // Using confirmed 240 pixel height from VisionConstants
        double actualHeight = VisionConstants.CAMERA_HEIGHT_PIXELS; // 240 pixels

        // Convert pixel to normalized coordinate (-1 to 1)
        // Note: Camera Y coordinates: 0 = top, 240 = bottom
        double normalizedY = (pixelY - (actualHeight / 2.0)) / (actualHeight / 2.0);

        // Convert to angle using camera FOV
        // Remove minus to fix Y mirroring
        return normalizedY * (VisionConstants.CAMERA_FOV_VERTICAL / 2.0);
    }

    /**
     * Check if a position is within intake mechanism reach
     * Can check boundary lines only OR full inside area based on configuration
     *
     * @param forwardDistance Distance forward from robot center (inches)
     * @param leftDistance Distance left from robot center (inches)
     * @return true if position is reachable by intake
     */
    private boolean isWithinIntakeReach(double forwardDistance, double leftDistance) {
        // Use correct constants in inches
        double slideReachInches = VisionConstants.INTAKE_SLIDE_MAX_EXTENSION_IN;
        double shoulderReachInches = VisionConstants.INTAKE_SHOULDER_REACH_IN;

        // Calculate sample position relative to INTAKE position (not robot center)
        // The D-shape stadium is centered on the intake, not the robot center
        double sampleRelativeToIntakeX = forwardDistance - VisionConstants.INTAKE_BASE_OFFSET_X;
        double sampleRelativeToIntakeY = leftDistance - VisionConstants.INTAKE_BASE_OFFSET_Y;

        // Case 1: Rectangle region
        if (sampleRelativeToIntakeX >= 0 && sampleRelativeToIntakeX <= slideReachInches && Math.abs(sampleRelativeToIntakeY) <= shoulderReachInches) {
            return true;
        }

        // Case 2: Left of intake base (only at intake base position)
        if (sampleRelativeToIntakeX < 0) {
            double dist = Math.hypot(sampleRelativeToIntakeX, sampleRelativeToIntakeY);
            if (dist <= shoulderReachInches) return true;
        }

        // Case 3: Beyond slide reach, semicircle at (slideReach, 0)
        if (sampleRelativeToIntakeX > slideReachInches) {
            double dx = sampleRelativeToIntakeX - slideReachInches;
            double dy = sampleRelativeToIntakeY;
            double dist = Math.hypot(dx, dy);
            if (dist <= shoulderReachInches) return true;
        }

        return false;
    }
}
