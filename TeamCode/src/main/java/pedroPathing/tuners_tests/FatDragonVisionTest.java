package pedroPathing.tuners_tests;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLStatus;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import pedroPathing.constants.CameraConstants;

/**
 * Test OpMode for FatDragon-based Limelight 3A sample detection
 * This OpMode tests the new FatDragon-based Python pipeline for sample detection
 *
 * Instructions:
 * 1. Upload fatdragon_sample_detection.py to Limelight pipeline 1
 * 2. Run this OpMode to test detection
 * 3. Use gamepad controls to switch pipelines and test
 */
@TeleOp(name = "FatDragon Vision Test", group = "Test")
public class FatDragonVisionTest extends LinearOpMode {

    private Limelight3A limelight;
    private ElapsedTime pipelineSwitchTimer = new ElapsedTime();
    private int targetPipeline = 1;
    private boolean pipelineSwitchRequested = false;

    @Override
    public void runOpMode() throws InterruptedException {
        telemetry.addData("Status", "Initializing FatDragon Vision Test...");
        telemetry.update();

        try {
            // Initialize Limelight
            limelight = hardwareMap.get(Limelight3A.class, "limelight");
            
            // Set telemetry transmission interval for better performance
            telemetry.setMsTransmissionInterval(50);
            
            // Start polling for data first
            limelight.start();

            // Use pipeline 0 (FatDragon pipeline) - no switching needed
            limelight.pipelineSwitch(0);
            targetPipeline = 0;

            telemetry.addData("Status", "✅ FatDragon Vision Test initialized!");
            telemetry.addData("Info", "Press PLAY to start test");
            telemetry.addData("Pipeline", "Using Pipeline 0 (FatDragon)");
            telemetry.update();
            
        } catch (Exception e) {
            telemetry.addData("❌ ERROR", "Failed to initialize Limelight 3A");
            telemetry.addData("Error Details", e.getMessage());
            telemetry.addData("Troubleshooting", "Check:");
            telemetry.addData("1.", "USB connection to Control Hub");
            telemetry.addData("2.", "Robot configuration file");
            telemetry.addData("3.", "Limelight power and boot status");
            telemetry.addData("4.", "FatDragon Python script uploaded to Pipeline 1");
            telemetry.update();
            
            waitForStart();
            return; // Exit if initialization failed
        }

        waitForStart();

        while (opModeIsActive()) {
            try {
                // Handle pipeline switching with verification
                handlePipelineSwitching();

                // Get Limelight status
                LLStatus status = limelight.getStatus();

                telemetry.addData("=== FATDRAGON VISION STATUS ===", "");
                telemetry.addData("Name", status.getName());
                telemetry.addData("Temperature", String.format("%.1f°C", status.getTemp()));
                telemetry.addData("CPU Usage", String.format("%.1f%%", status.getCpu()));
                telemetry.addData("FPS", String.format("%d", (int)status.getFps()));

                // Enhanced pipeline status with switching feedback
                int currentPipeline = status.getPipelineIndex();
                String pipelineStatus = String.format("Current: %d, Target: %d, Type: %s",
                        currentPipeline, targetPipeline, status.getPipelineType());
                if (currentPipeline != targetPipeline) {
                    pipelineStatus += " ⚠️ SWITCHING...";
                } else {
                    pipelineStatus += " ✅ ACTIVE";
                }
                telemetry.addData("Pipeline", pipelineStatus);

                // Get latest results
                LLResult result = limelight.getLatestResult();
                
                telemetry.addData("=== SAMPLE DETECTION RESULTS ===", "");
                
                if (result != null) {
                    // Display latency information
                    double captureLatency = result.getCaptureLatency();
                    double targetingLatency = result.getTargetingLatency();
                    double parseLatency = result.getParseLatency();
                    
                    telemetry.addData("Total Latency", String.format("%.1fms", captureLatency + targetingLatency));
                    telemetry.addData("Parse Latency", String.format("%.1fms", parseLatency));
                    
                    // Display Python output from FatDragon algorithm
                    double[] pythonOutput = result.getPythonOutput();
                    if (pythonOutput != null && pythonOutput.length >= 8) {
                        int samplesDetected = (int)pythonOutput[0];
                        
                        if (samplesDetected > 0) {
                            telemetry.addData("🎯 SAMPLES DETECTED", String.format("%d samples", samplesDetected));
                            
                            // Best sample data
                            double sampleX = pythonOutput[1];
                            double sampleY = pythonOutput[2];
                            double sampleAngle = pythonOutput[3];
                            int colorCode = (int)pythonOutput[4];
                            int quadrant = (int)pythonOutput[5];
                            int confidence = (int)pythonOutput[6];
                            int distance = (int)pythonOutput[7];
                            
                            // Convert color code to name
                            String colorName = "UNKNOWN";
                            switch (colorCode) {
                                case 1: colorName = "🔴 RED"; break;
                                case 2: colorName = "🟡 YELLOW"; break;
                                case 3: colorName = "🔵 BLUE"; break;
                            }
                            
                            // Convert quadrant to description
                            String quadrantDesc = "Center";
                            switch (quadrant) {
                                case 1: quadrantDesc = "Top"; break;
                                case 2: quadrantDesc = "Bottom"; break;
                            }
                            
                            telemetry.addData("Best Sample Color", colorName);
                            telemetry.addData("Position", String.format("X:%.0f Y:%.0f", sampleX, sampleY));
                            telemetry.addData("Angle", String.format("%.1f°", sampleAngle));
                            telemetry.addData("Quadrant", quadrantDesc);
                            telemetry.addData("Confidence", String.format("%d%%", confidence));
                            telemetry.addData("Distance", String.format("%d inches", distance));

                            // Calculate field angles for robot navigation using proper camera constants
                            double fieldAngleX = calculateFieldAngleX(sampleX);
                            double fieldAngleY = calculateFieldAngleY(sampleY);
                            telemetry.addData("Field Angles", String.format("TX:%.1f° TY:%.1f°", fieldAngleX, fieldAngleY));

                            // Calculate improved distance using camera geometry
                            double improvedDistance = calculateImprovedDistance(sampleX, sampleY);
                            telemetry.addData("Improved Distance", String.format("%.1f inches", improvedDistance));

                            // Calculate robot-relative position
                            double[] robotPosition = calculateRobotRelativePosition(sampleX, sampleY, improvedDistance);
                            telemetry.addData("Robot Relative Pos", String.format("Forward:%.1f Left:%.1f", robotPosition[0], robotPosition[1]));
                            telemetry.addData("Position Meaning", String.format("X=%.1f(fwd/back) Y=%.1f(left/right)", robotPosition[0], robotPosition[1]));
                            
                        } else {
                            telemetry.addData("Sample Status", "❌ NO SAMPLES DETECTED");
                        }
                        
                        // Raw Python output for debugging
                        telemetry.addData("Raw Python Data", String.format("[%.0f,%.0f,%.0f,%.1f,%d,%d,%d,%d]",
                                pythonOutput[0], pythonOutput[1], pythonOutput[2], pythonOutput[3],
                                (int)pythonOutput[4], (int)pythonOutput[5], (int)pythonOutput[6], (int)pythonOutput[7]));
                        
                    } else {
                        telemetry.addData("Python Output", "❌ NO PYTHON DATA");
                        telemetry.addData("Troubleshooting", "Check FatDragon script in Pipeline 1");
                    }
                    
                    // Basic targeting data
                    if (result.isValid()) {
                        telemetry.addData("Basic Target", "✅ DETECTED");
                        telemetry.addData("TX (Horizontal)", String.format("%.2f°", result.getTx()));
                        telemetry.addData("TY (Vertical)", String.format("%.2f°", result.getTy()));
                        telemetry.addData("TA (Area)", String.format("%.2f%%", result.getTa()));
                    } else {
                        telemetry.addData("Basic Target", "❌ NO BASIC TARGET");
                    }
                    
                } else {
                    telemetry.addData("Data Status", "❌ NO DATA AVAILABLE");
                    telemetry.addData("Troubleshooting", "Check Limelight web interface");
                }
                
                // Pipeline troubleshooting information
                telemetry.addData("=== TROUBLESHOOTING ===", "");
                if (status.getPipelineIndex() == 0) {
                    telemetry.addData("Pipeline 0 Info", "FatDragon Python script");
                    if (result != null && result.getPythonOutput() == null) {
                        telemetry.addData("⚠️ Warning", "No Python output - script may not be loaded");
                        telemetry.addData("Solution", "Upload fatdragon_sample_detection.py to Pipeline 0");
                    } else if (result != null && result.getPythonOutput() != null) {
                        telemetry.addData("✅ Python Script", "Loaded and running on Pipeline 0");
                    }
                }

                // Camera constants verification
                telemetry.addData("=== CAMERA CONSTANTS ===", "");
                telemetry.addData("Camera Height", String.format("%.1f inches", CameraConstants.CAMERA_HEIGHT));
                telemetry.addData("Camera Tilt", String.format("%.1f degrees", CameraConstants.CAMERA_TILT_ANGLE));
                telemetry.addData("Camera FOV", String.format("H:%.1f° V:%.1f°",
                    CameraConstants.CAMERA_FOV_HORIZONTAL, CameraConstants.CAMERA_FOV_VERTICAL));
                telemetry.addData("Camera Offset", String.format("X:%.1f Y:%.1f",
                    CameraConstants.CAMERA_OFFSET_X, CameraConstants.CAMERA_OFFSET_Y));

                // Control instructions
                telemetry.addData("=== CONTROLS ===", "");
                telemetry.addData("Info", "FatDragon script is on Pipeline 0");
                telemetry.addData("X", "Toggle LED Mode");
                telemetry.addData("Y", "Force LED On");
                telemetry.addData("DPAD_UP", "Run Pipeline Diagnostics");
                
                // Handle gamepad controls (FatDragon is on Pipeline 0)
                // No pipeline switching needed since FatDragon is on Pipeline 0
                if (gamepad1.x) {
                    // Toggle LED mode via HTTP (blink mode)
                    telemetry.addData("LED Control", "LED Blink Mode");
                }
                if (gamepad1.y) {
                    // Force LED on via HTTP
                    telemetry.addData("LED Control", "LED Force On");
                }
                if (gamepad1.dpad_up) {
                    runPipelineDiagnostics();
                }
                
            } catch (Exception e) {
                telemetry.addData("❌ RUNTIME ERROR", e.getMessage());
                telemetry.addData("Error Details", e.toString());
            }
            
            telemetry.update();
        }
        
        // Stop Limelight when OpMode ends
        if (limelight != null) {
            limelight.stop();
        }
    }

    /**
     * Robust pipeline switching with verification and retry logic
     */
    private void switchPipelineRobust(int pipeline) {
        targetPipeline = pipeline;
        pipelineSwitchRequested = true;
        pipelineSwitchTimer.reset();

        // Attempt initial switch
        try {
            limelight.pipelineSwitch(pipeline);
        } catch (Exception e) {
            telemetry.addData("Pipeline Switch Error", e.getMessage());
        }
    }

    /**
     * Handle pipeline switching verification and retry logic
     */
    private void handlePipelineSwitching() {
        if (!pipelineSwitchRequested) {
            return;
        }

        try {
            LLStatus status = limelight.getStatus();
            int currentPipeline = status.getPipelineIndex();

            // Check if pipeline switch was successful
            if (currentPipeline == targetPipeline) {
                pipelineSwitchRequested = false;
                telemetry.addData("Pipeline Switch", "✅ Successfully switched to " + targetPipeline);
            } else if (pipelineSwitchTimer.seconds() > 2.0) {
                // Retry after 2 seconds
                telemetry.addData("Pipeline Switch", "⚠️ Retrying switch to " + targetPipeline);
                limelight.pipelineSwitch(targetPipeline);
                pipelineSwitchTimer.reset();
            } else {
                // Still waiting for switch
                telemetry.addData("Pipeline Switch", String.format("⏳ Switching to %d... (%.1fs)",
                        targetPipeline, pipelineSwitchTimer.seconds()));
            }
        } catch (Exception e) {
            telemetry.addData("Pipeline Status Error", e.getMessage());
            // Reset switch request on error
            if (pipelineSwitchTimer.seconds() > 5.0) {
                pipelineSwitchRequested = false;
            }
        }
    }

    /**
     * Run comprehensive pipeline diagnostics
     */
    private void runPipelineDiagnostics() {
        telemetry.addData("=== PIPELINE DIAGNOSTICS ===", "");

        try {
            LLStatus status = limelight.getStatus();
            LLResult result = limelight.getLatestResult();

            // Basic connectivity
            telemetry.addData("✅ Limelight Connected", "Yes");
            telemetry.addData("Current Pipeline", status.getPipelineIndex());
            telemetry.addData("Pipeline Type", status.getPipelineType());

            // Test pipeline 0 switch
            telemetry.addData("Testing Pipeline 0", "Switching...");
            limelight.pipelineSwitch(0);
            sleep(1000);

            LLStatus status0 = limelight.getStatus();
            if (status0.getPipelineIndex() == 0) {
                telemetry.addData("✅ Pipeline 0", "Switch successful");
            } else {
                telemetry.addData("❌ Pipeline 0", "Switch failed");
            }

            // Test pipeline 1 switch
            telemetry.addData("Testing Pipeline 1", "Switching...");
            limelight.pipelineSwitch(1);
            sleep(2000); // Give more time for Python pipeline

            LLStatus status1 = limelight.getStatus();
            LLResult result1 = limelight.getLatestResult();

            if (status1.getPipelineIndex() == 1) {
                telemetry.addData("✅ Pipeline 1", "Switch successful");

                if (result1 != null && result1.getPythonOutput() != null) {
                    telemetry.addData("✅ Python Script", "Loaded and running");
                } else {
                    telemetry.addData("❌ Python Script", "Not loaded or not running");
                    telemetry.addData("Solution", "Upload fatdragon_sample_detection.py to Pipeline 1");
                }
            } else {
                telemetry.addData("❌ Pipeline 1", "Switch failed - Pipeline may not exist");
                telemetry.addData("Solution", "Create Pipeline 1 in Limelight web interface");
            }

        } catch (Exception e) {
            telemetry.addData("❌ Diagnostics Error", e.getMessage());
        }

        telemetry.addData("Diagnostics", "Complete - Check results above");
        telemetry.update();
        sleep(5000); // Show results for 5 seconds
    }

    // ===== CAMERA CALCULATION METHODS =====

    /**
     * Calculate horizontal field angle from pixel X coordinate
     * @param pixelX X coordinate in pixels (0-320 for Limelight 3A)
     * @return Horizontal angle in degrees
     */
    private double calculateFieldAngleX(double pixelX) {
        // Convert pixel coordinate to normalized coordinate (-1 to 1)
        double normalizedX = (pixelX - (CameraConstants.CAMERA_WIDTH / 2.0)) / (CameraConstants.CAMERA_WIDTH / 2.0);

        // Convert to angle using camera FOV
        return normalizedX * (CameraConstants.CAMERA_FOV_HORIZONTAL / 2.0);
    }

    /**
     * Calculate vertical field angle from pixel Y coordinate
     * @param pixelY Y coordinate in pixels (0-240 for Limelight 3A)
     * @return Vertical angle in degrees
     */
    private double calculateFieldAngleY(double pixelY) {
        // Convert pixel coordinate to normalized coordinate (-1 to 1)
        double normalizedY = (pixelY - (CameraConstants.CAMERA_HEIGHT_PIXELS / 2.0)) / (CameraConstants.CAMERA_HEIGHT_PIXELS / 2.0);

        // Convert to angle using camera FOV (note: Y is inverted in camera coordinates)
        return -normalizedY * (CameraConstants.CAMERA_FOV_VERTICAL / 2.0);
    }

    /**
     * Calculate improved distance using camera geometry and constants
     * @param pixelX X coordinate in pixels
     * @param pixelY Y coordinate in pixels
     * @return Distance in inches
     */
    private double calculateImprovedDistance(double pixelX, double pixelY) {
        try {
            // Get vertical angle to target
            double verticalAngle = calculateFieldAngleY(pixelY);

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
     * @param pixelX X coordinate in pixels
     * @param pixelY Y coordinate in pixels
     * @param distance Distance to sample in inches
     * @return Array with [X, Y] position relative to robot center
     *
     * Coordinate System:
     * - X-axis: Forward/Backward (+ = forward from robot, - = behind robot)
     * - Y-axis: Left/Right (+ = left of robot, - = right of robot)
     * - Origin: Robot center
     */
    private double[] calculateRobotRelativePosition(double pixelX, double pixelY, double distance) {
        try {
            // Get horizontal angle to target
            double horizontalAngle = calculateFieldAngleX(pixelX);

            // Calculate position relative to camera using standard robot coordinates
            // X = forward/backward, Y = left/right
            double cameraRelativeX = distance * Math.cos(Math.toRadians(horizontalAngle)); // Forward distance
            double cameraRelativeY = distance * Math.sin(Math.toRadians(horizontalAngle)); // Left/right distance

            // Adjust for camera offset from robot center
            // CAMERA_OFFSET_X = how far forward the camera is from robot center
            // CAMERA_OFFSET_Y = how far left the camera is from robot center
            double robotRelativeX = cameraRelativeX + CameraConstants.CAMERA_OFFSET_X;
            double robotRelativeY = cameraRelativeY + CameraConstants.CAMERA_OFFSET_Y;

            return new double[]{robotRelativeX, robotRelativeY};
        } catch (Exception e) {
            return new double[]{0.0, 0.0};
        }
    }
}
