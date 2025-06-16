package pedroPathing.tuners_tests;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLStatus;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import pedroPathing.constants.CameraConstants;

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
                            
                            // Calculate distance using camera constants
                            double distance = calculateDistance(pixelX, pixelY);
                            telemetry.addData("Calculated Distance", String.format("%.1f inches", distance));
                            
                            // Calculate robot-relative position
                            double[] robotPosition = calculateRobotPosition(pixelX, pixelY, distance);
                            telemetry.addData("Robot Relative Position", 
                                String.format("Forward:%.1f Left:%.1f", robotPosition[0], robotPosition[1]));
                            
                            // Show field angles for reference
                            double[] fieldAngles = calculateFieldAngles(pixelX, pixelY);
                            telemetry.addData("Field Angles", 
                                String.format("Horizontal:%.1f° Vertical:%.1f°", fieldAngles[0], fieldAngles[1]));
                            
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
                telemetry.addData("Camera Height", String.format("%.1f inches", CameraConstants.CAMERA_HEIGHT));
                telemetry.addData("Camera Tilt", String.format("%.1f degrees", CameraConstants.CAMERA_TILT_ANGLE));
                telemetry.addData("Camera Offset", String.format("X:%.1f Y:%.1f", 
                    CameraConstants.CAMERA_OFFSET_X, CameraConstants.CAMERA_OFFSET_Y));
                telemetry.addData("Sample Height", String.format("%.1f inches", CameraConstants.SAMPLE_HEIGHT));

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
     * @param pixelX X coordinate in pixels (0-320)
     * @param pixelY Y coordinate in pixels (0-240)
     * @return Distance in inches
     */
    private double calculateDistance(double pixelX, double pixelY) {
        try {
            // Convert pixel Y to vertical angle
            double verticalAngle = calculateVerticalAngle(pixelY);
            
            // Adjust for camera tilt (camera is tilted down)
            double effectiveAngle = Math.toRadians(verticalAngle + CameraConstants.CAMERA_TILT_ANGLE);
            
            // Calculate distance using trigonometry
            // distance = (camera_height - sample_height) / tan(angle)
            double heightDifference = CameraConstants.CAMERA_HEIGHT - CameraConstants.SAMPLE_HEIGHT;
            
            if (Math.abs(effectiveAngle) > 0.01) { // Avoid division by zero
                double distance = heightDifference / Math.tan(Math.abs(effectiveAngle));
                
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
     * 
     * @param pixelX X coordinate in pixels
     * @param pixelY Y coordinate in pixels  
     * @param distance Distance to sample in inches
     * @return Array with [forward/backward, left/right] position relative to robot center
     */
    private double[] calculateRobotPosition(double pixelX, double pixelY, double distance) {
        try {
            // Get horizontal angle to target
            double horizontalAngle = calculateHorizontalAngle(pixelX);
            
            // Calculate position relative to camera
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
     * @param pixelX X coordinate in pixels (0-320)
     * @return Horizontal angle in degrees
     */
    private double calculateHorizontalAngle(double pixelX) {
        // Convert pixel to normalized coordinate (-1 to 1)
        double normalizedX = (pixelX - (CameraConstants.CAMERA_WIDTH / 2.0)) / (CameraConstants.CAMERA_WIDTH / 2.0);
        
        // Convert to angle using camera FOV
        return normalizedX * (CameraConstants.CAMERA_FOV_HORIZONTAL / 2.0);
    }

    /**
     * Convert pixel Y coordinate to vertical angle
     * 
     * @param pixelY Y coordinate in pixels (0-240)
     * @return Vertical angle in degrees
     */
    private double calculateVerticalAngle(double pixelY) {
        // Convert pixel to normalized coordinate (-1 to 1)
        // Note: Y is inverted in camera coordinates (0 = top, 240 = bottom)
        double normalizedY = (pixelY - (CameraConstants.CAMERA_HEIGHT_PIXELS / 2.0)) / (CameraConstants.CAMERA_HEIGHT_PIXELS / 2.0);
        
        // Convert to angle using camera FOV (invert Y for proper field coordinates)
        return -normalizedY * (CameraConstants.CAMERA_FOV_VERTICAL / 2.0);
    }
}
