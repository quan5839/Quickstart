package pedroPathing.hardware;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import pedroPathing.constants.IntakeConstants;
import pedroPathing.constants.VisionConstants;

/**
 * Simple Vision-Guided Intake Positioning
 * 
 * This class provides basic sample positioning using vision data from the Limelight 3A.
 * It only positions the intake (slide, turret, wrist) toward the sample.
 * Use your existing state machine for the actual pickup sequence.
 * 
 * Usage:
 * 1. Call pointTowardSample() to position intake toward detected sample
 * 2. Use your existing state machine to execute pickup
 */
public class SimpleVisionIntake {
    
    private final RobotHardware robot;
    private final VisionSystem visionSystem;
    private final OpMode opMode;
    
    // Simple state tracking
    private boolean isPositioning = false;
    private double targetTurretPosition = IntakeConstants.TURRET_MIDDLE;
    private double targetSlidePosition = IntakeConstants.SLIDE_MIN;
    private double targetWristPosition = IntakeConstants.WRIST_MIDDLE;
    
    public SimpleVisionIntake(RobotHardware robot, OpMode opMode) {
        this.robot = robot;
        this.visionSystem = robot.visionSystem;
        this.opMode = opMode;
    }
    
    /**
     * Point the intake toward the detected sample
     * @return true if sample detected and intake positioned, false if no sample
     */
    public boolean pointTowardSample() {
        // Enable color recognition for FatDragon pipeline
        visionSystem.setColorRecognitionEnabled(true);
        
        // Check if we have a sample
        if (!visionSystem.isColorSampleDetected() && !visionSystem.isTargetVisible()) {
            opMode.telemetry.addData("Vision Intake", "❌ No sample detected");
            return false;
        }
        
        try {
            // Get sample position from vision
            double[] samplePosition = calculateSamplePosition();
            if (samplePosition == null) {
                opMode.telemetry.addData("Vision Intake", "❌ Invalid sample position");
                return false;
            }
            
            double sampleX = samplePosition[0]; // Forward/backward
            double sampleY = samplePosition[1]; // Left/right
            double sampleDistance = Math.sqrt(sampleX * sampleX + sampleY * sampleY);
            double sampleAngle = Math.toDegrees(Math.atan2(sampleY, sampleX));
            
            // Calculate intake positions
            calculateIntakePositions(sampleDistance, sampleAngle);
            
            // Position the intake
            robot.intake.setIntakeTurretPosition(targetTurretPosition);
            robot.intake.setIntakeSlidePosition(targetSlidePosition);
            robot.intake.setIntakeWristPosition(targetWristPosition);
            
            isPositioning = true;
            
            opMode.telemetry.addData("Vision Intake", "✅ Positioned toward sample");
            opMode.telemetry.addData("Sample Distance", String.format("%.1f inches", sampleDistance));
            opMode.telemetry.addData("Sample Angle", String.format("%.1f degrees", sampleAngle));
            opMode.telemetry.addData("Turret Position", String.format("%.3f", targetTurretPosition));
            opMode.telemetry.addData("Slide Position", String.format("%.3f", targetSlidePosition));
            
            return true;
            
        } catch (Exception e) {
            opMode.telemetry.addData("Vision Intake Error", e.getMessage());
            return false;
        }
    }
    
    /**
     * Reset intake to safe/home position
     */
    public void resetToHome() {
        robot.intake.setIntakeTurretPosition(IntakeConstants.TURRET_MIDDLE);
        robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MIN);
        robot.intake.setIntakeWristPosition(IntakeConstants.WRIST_MIDDLE);
        isPositioning = false;
        opMode.telemetry.addData("Vision Intake", "🏠 Reset to home position");
    }
    
    /**
     * Check if intake is currently positioning toward a sample
     */
    public boolean isPositioning() {
        return isPositioning;
    }
    
    /**
     * Get the calculated positions for telemetry/debugging
     */
    public double[] getTargetPositions() {
        return new double[]{targetTurretPosition, targetSlidePosition, targetWristPosition};
    }
    
    /**
     * Calculate sample position from vision data
     */
    private double[] calculateSamplePosition() {
        try {
            double pixelX, pixelY;
            
            // Get TX/TY angles from vision system
            double tx = visionSystem.getTargetX(); // Horizontal angle
            double ty = visionSystem.getTargetY(); // Vertical angle
            
            // Convert angles to pixel coordinates for distance calculation
            pixelX = (tx / (VisionConstants.CAMERA_FOV_HORIZONTAL / 2.0)) * (VisionConstants.CAMERA_WIDTH / 2.0) + (VisionConstants.CAMERA_WIDTH / 2.0);
            pixelY = (-ty / (VisionConstants.CAMERA_FOV_VERTICAL / 2.0)) * (VisionConstants.CAMERA_HEIGHT_PIXELS / 2.0) + (VisionConstants.CAMERA_HEIGHT_PIXELS / 2.0);
            
            // Calculate distance using camera geometry
            double distance = calculateDistance(pixelX, pixelY);
            
            // Calculate robot-relative position
            double horizontalAngle = tx; // Use TX directly
            double robotRelativeX = distance * Math.cos(Math.toRadians(horizontalAngle));
            double robotRelativeY = distance * Math.sin(Math.toRadians(horizontalAngle));
            
            // Adjust for camera offset
            robotRelativeX += VisionConstants.CAMERA_OFFSET_X;
            robotRelativeY += VisionConstants.CAMERA_OFFSET_Y;
            
            return new double[]{robotRelativeX, robotRelativeY};
            
        } catch (Exception e) {
            opMode.telemetry.addData("Position Calc Error", e.getMessage());
            return null;
        }
    }
    
    /**
     * Calculate distance using camera geometry
     */
    private double calculateDistance(double pixelX, double pixelY) {
        try {
            // Calculate vertical angle
            double normalizedY = (pixelY - (VisionConstants.CAMERA_HEIGHT_PIXELS / 2.0)) / (VisionConstants.CAMERA_HEIGHT_PIXELS / 2.0);
            double verticalAngle = -normalizedY * (VisionConstants.CAMERA_FOV_VERTICAL / 2.0);

            // Adjust for camera tilt
            double effectiveAngle = Math.toRadians(verticalAngle + VisionConstants.CAMERA_TILT_ANGLE);
            
            // Calculate distance using trigonometry
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
     * Calculate intake servo positions based on sample distance and angle using asymmetric limits
     */
    private void calculateIntakePositions(double distance, double angle) {
        // Calculate turret position based on sample angle with asymmetric limits
        double clampedAngle;
        if (angle >= 0) {
            // Left side - use left max angle
            clampedAngle = Math.min(angle, IntakeConstants.AUTO_INTAKE_TURRET_LEFT_MAX_ANGLE);
        } else {
            // Right side - use right max angle
            clampedAngle = Math.max(angle, -IntakeConstants.AUTO_INTAKE_TURRET_RIGHT_MAX_ANGLE);
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

        targetTurretPosition = IntakeConstants.TURRET_MIDDLE + (normalizedAngle * turretRange / 2.0);
        
        // Clamp turret position to safe limits
        targetTurretPosition = Math.max(IntakeConstants.TURRET_RIGHT, 
                                      Math.min(targetTurretPosition, IntakeConstants.TURRET_LEFT));
        
        // Calculate slide position based on distance
        // Limit distance to safe reach
        double maxReach = IntakeConstants.AUTO_INTAKE_MAX_SLIDE_REACH;
        double clampedDistance = Math.max(IntakeConstants.AUTO_INTAKE_MIN_DISTANCE, 
                                        Math.min(distance, maxReach));
        
        // Map distance to slide position
        double slideExtension = clampedDistance / maxReach;
        targetSlidePosition = IntakeConstants.SLIDE_MIN - (slideExtension * (IntakeConstants.SLIDE_MIN - IntakeConstants.SLIDE_MAX));
        
        // Keep wrist centered for now (you can add wrist angle calculation later if needed)
        targetWristPosition = IntakeConstants.WRIST_MIDDLE;
        
        String angleLimit = angle >= 0 ?
            String.format("L:%.0f°", IntakeConstants.AUTO_INTAKE_TURRET_LEFT_MAX_ANGLE) :
            String.format("R:%.0f°", IntakeConstants.AUTO_INTAKE_TURRET_RIGHT_MAX_ANGLE);
        opMode.telemetry.addData("Calc Angle", String.format("%.1f° (clamped from %.1f°, limit %s)", clampedAngle, angle, angleLimit));
        opMode.telemetry.addData("Calc Distance", String.format("%.1f\" (clamped from %.1f\")", clampedDistance, distance));
    }
    
    /**
     * Check if a sample is currently detected
     */
    public boolean isSampleDetected() {
        return visionSystem.isColorSampleDetected() || visionSystem.isTargetVisible();
    }
    
    /**
     * Get detected sample color
     */
    public String getSampleColor() {
        if (visionSystem.isColorSampleDetected()) {
            return visionSystem.getDetectedColor().toString();
        }
        return "UNKNOWN";
    }
    
    /**
     * Get current vision angles for debugging
     */
    public double[] getVisionAngles() {
        return new double[]{visionSystem.getTargetX(), visionSystem.getTargetY()};
    }
}
