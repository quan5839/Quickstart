package pedroPathing.util;

import pedroPathing.constants.IntakeConstants;
import pedroPathing.constants.VisionConstants;

/**
 * Stadium Area Calculator for 5 DOF Intake System
 * 
 * This utility class calculates the stadium-shaped reachable area for the intake system
 * based on the physical constraints of the 5 DOF mechanism:
 * - Slide: 40cm (15.75") forward/backward movement
 * - Turret: 270° rotation with limits
 * - Shoulder: 17cm (6.69") radius rotation
 * - Elbow: Up/down articulation
 * - Wrist: 270° rotation
 * 
 * Stadium Shape Description:
 * - Forward semicircle: slide extended + shoulder rotation creates curved front
 * - Rectangle sides: slide movement with turret rotation creates straight sides
 * - No backward semicircle: intake is forward-facing only
 * 
 * Coordinate System:
 * - X: Forward/backward (positive = forward)
 * - Y: Left/right (positive = left)
 * - Origin: Robot center
 * 
 * @author Baron Henderson - 20077 The Indubitables
 * @version 1.0, 12/30/2024
 */
public class StadiumAreaCalculator {
    
    /**
     * Check if a position is within the stadium-shaped reachable area
     * @param x Forward/backward position (inches, positive = forward)
     * @param y Left/right position (inches, positive = left)
     * @return true if position is reachable
     */
    public static boolean isPositionReachable(double x, double y) {
        if (!IntakeConstants.ENABLE_STADIUM_AREA_CHECK) {
            return true; // Skip check if disabled
        }
        
        // Apply intake base offset
        double adjustedX = x - VisionConstants.INTAKE_BASE_OFFSET_X;
        double adjustedY = y - VisionConstants.INTAKE_BASE_OFFSET_Y;
        
        double distance = Math.sqrt(adjustedX * adjustedX + adjustedY * adjustedY);
        double angle = Math.atan2(adjustedY, adjustedX);
        double angleDegrees = Math.toDegrees(angle);
        
        // Basic distance and angle checks
        if (distance > IntakeConstants.AUTO_INTAKE_MAX_SLIDE_REACH) {
            return false;
        }
        
        if (distance < IntakeConstants.AUTO_INTAKE_MIN_DISTANCE) {
            return false;
        }
        
        // Check turret angle limits with asymmetric constraints
        if (angleDegrees >= 0) {
            // Left side - check against left max angle
            if (angleDegrees > IntakeConstants.AUTO_INTAKE_TURRET_LEFT_MAX_ANGLE) {
                return false;
            }
        } else {
            // Right side - check against right max angle
            if (angleDegrees < -IntakeConstants.AUTO_INTAKE_TURRET_RIGHT_MAX_ANGLE) {
                return false;
            }
        }
        
        // Stadium area calculation
        return isInStadiumBoundary(adjustedX, adjustedY);
    }
    
    /**
     * Check if position is within the stadium boundary
     * @param x Adjusted X position
     * @param y Adjusted Y position
     * @return true if within stadium boundary
     */
    private static boolean isInStadiumBoundary(double x, double y) {
        double absY = Math.abs(y);
        
        // Forward semicircle area (x > shoulder_radius)
        if (x > IntakeConstants.SHOULDER_RADIUS_INCHES) {
            return isInForwardSemicircle(x, y);
        }
        
        // Rectangle sides area (0 <= x <= shoulder_radius)
        else if (x >= -IntakeConstants.STADIUM_SAFETY_MARGIN) {
            return isInRectangleSides(x, absY);
        }
        
        // Behind robot (x < 0) - not reachable with forward-facing intake
        else {
            return false;
        }
    }
    
    /**
     * Check if position is in the forward semicircle area
     * @param x X position
     * @param y Y position
     * @return true if in forward semicircle
     */
    private static boolean isInForwardSemicircle(double x, double y) {
        // Calculate the center of the semicircle (at shoulder radius)
        double centerX = IntakeConstants.SHOULDER_RADIUS_INCHES;
        double centerY = 0.0;
        
        // Distance from semicircle center
        double distanceFromCenter = Math.sqrt(
            (x - centerX) * (x - centerX) + 
            (y - centerY) * (y - centerY)
        );
        
        // Maximum radius is slide length
        double maxRadius = IntakeConstants.SLIDE_LENGTH_INCHES - IntakeConstants.STADIUM_SAFETY_MARGIN;
        
        return distanceFromCenter <= maxRadius;
    }
    
    /**
     * Check if position is in the rectangle sides area
     * @param x X position
     * @param absY Absolute Y position
     * @return true if in rectangle sides
     */
    private static boolean isInRectangleSides(double x, double absY) {
        // Maximum Y reach is slide length
        double maxY = IntakeConstants.SLIDE_LENGTH_INCHES - IntakeConstants.STADIUM_SAFETY_MARGIN;
        
        return absY <= maxY;
    }
    
    /**
     * Get the closest reachable position to a target
     * @param targetX Target X position
     * @param targetY Target Y position
     * @return [x, y] of closest reachable position
     */
    public static double[] getClosestReachablePosition(double targetX, double targetY) {
        if (isPositionReachable(targetX, targetY)) {
            return new double[]{targetX, targetY};
        }
        
        // Apply intake base offset
        double adjustedX = targetX - VisionConstants.INTAKE_BASE_OFFSET_X;
        double adjustedY = targetY - VisionConstants.INTAKE_BASE_OFFSET_Y;
        
        double distance = Math.sqrt(adjustedX * adjustedX + adjustedY * adjustedY);
        double angle = Math.atan2(adjustedY, adjustedX);
        double angleDegrees = Math.toDegrees(angle);
        
        // Clamp angle to asymmetric limits
        if (angleDegrees >= 0) {
            // Left side - use left max angle
            angleDegrees = Math.min(angleDegrees, IntakeConstants.AUTO_INTAKE_TURRET_LEFT_MAX_ANGLE);
        } else {
            // Right side - use right max angle
            angleDegrees = Math.max(angleDegrees, -IntakeConstants.AUTO_INTAKE_TURRET_RIGHT_MAX_ANGLE);
        }
        angle = Math.toRadians(angleDegrees);
        
        // Clamp distance to stadium boundary
        double maxReachAtAngle = getMaxReachAtAngle(angleDegrees);
        double clampedDistance = Math.max(IntakeConstants.AUTO_INTAKE_MIN_DISTANCE,
                                         Math.min(distance, maxReachAtAngle));
        
        // Convert back to robot coordinates
        double clampedX = clampedDistance * Math.cos(angle) + VisionConstants.INTAKE_BASE_OFFSET_X;
        double clampedY = clampedDistance * Math.sin(angle) + VisionConstants.INTAKE_BASE_OFFSET_Y;
        
        return new double[]{clampedX, clampedY};
    }
    
    /**
     * Get maximum reach distance at a specific angle
     * @param angleDegrees Angle in degrees
     * @return Maximum reach distance in inches
     */
    public static double getMaxReachAtAngle(double angleDegrees) {
        double angleRad = Math.toRadians(angleDegrees);
        double x = IntakeConstants.AUTO_INTAKE_MAX_SLIDE_REACH * Math.cos(angleRad);
        double y = IntakeConstants.AUTO_INTAKE_MAX_SLIDE_REACH * Math.sin(angleRad);
        
        // Check if this position is in the stadium area
        if (isInStadiumBoundary(x, y)) {
            return IntakeConstants.AUTO_INTAKE_MAX_SLIDE_REACH;
        }
        
        // Binary search for maximum reachable distance at this angle
        double minDist = IntakeConstants.AUTO_INTAKE_MIN_DISTANCE;
        double maxDist = IntakeConstants.AUTO_INTAKE_MAX_SLIDE_REACH;
        double tolerance = 0.1; // inches
        
        while (maxDist - minDist > tolerance) {
            double testDist = (minDist + maxDist) / 2.0;
            double testX = testDist * Math.cos(angleRad);
            double testY = testDist * Math.sin(angleRad);
            
            if (isInStadiumBoundary(testX, testY)) {
                minDist = testDist;
            } else {
                maxDist = testDist;
            }
        }
        
        return minDist - IntakeConstants.STADIUM_SAFETY_MARGIN;
    }
    
    /**
     * Calculate servo positions for a target position
     * @param x Target X position
     * @param y Target Y position
     * @return [turretPosition, slidePosition, shoulderPosition, elbowPosition, wristPosition]
     */
    public static double[] calculateServoPositions(double x, double y) {
        // Ensure position is reachable
        double[] reachablePos = getClosestReachablePosition(x, y);
        double reachableX = reachablePos[0];
        double reachableY = reachablePos[1];
        
        double distance = Math.sqrt(reachableX * reachableX + reachableY * reachableY);
        double angle = Math.atan2(reachableY, reachableX);
        double angleDegrees = Math.toDegrees(angle);
        
        // Calculate turret position using asymmetric angle scaling
        double turretRange = IntakeConstants.TURRET_LEFT - IntakeConstants.TURRET_RIGHT;
        double normalizedAngle;

        if (angleDegrees >= 0) {
            // Left side: scale by left max angle
            normalizedAngle = angleDegrees / IntakeConstants.AUTO_INTAKE_TURRET_LEFT_MAX_ANGLE;
        } else {
            // Right side: scale by right max angle
            normalizedAngle = angleDegrees / IntakeConstants.AUTO_INTAKE_TURRET_RIGHT_MAX_ANGLE;
        }

        double turretPosition = IntakeConstants.TURRET_MIDDLE + (normalizedAngle * turretRange / 2.0);
        turretPosition = Math.max(IntakeConstants.TURRET_RIGHT, 
                                 Math.min(turretPosition, IntakeConstants.TURRET_LEFT));
        
        // Calculate slide position
        double slideExtension = Math.min(distance / IntakeConstants.AUTO_INTAKE_MAX_SLIDE_REACH, 1.0);
        double slidePosition = IntakeConstants.SLIDE_MIN - 
            (slideExtension * (IntakeConstants.SLIDE_MIN - IntakeConstants.SLIDE_MAX));
        
        // Use default positions for shoulder, elbow, and wrist (can be enhanced with kinematics)
        double shoulderPosition = IntakeConstants.SHOULDER_GRAB;
        double elbowPosition = IntakeConstants.ELBOW_GRAB;
        double wristPosition = IntakeConstants.WRIST_MIDDLE;
        
        return new double[]{turretPosition, slidePosition, shoulderPosition, elbowPosition, wristPosition};
    }
    
    /**
     * Get stadium area information for telemetry
     * @return String array with stadium area details
     */
    public static String[] getStadiumAreaInfo() {
        return new String[]{
            String.format("Slide Length: %.1f\"", IntakeConstants.SLIDE_LENGTH_INCHES),
            String.format("Shoulder Radius: %.1f\"", IntakeConstants.SHOULDER_RADIUS_INCHES),
            String.format("Max Reach: %.1f\"", IntakeConstants.AUTO_INTAKE_MAX_SLIDE_REACH),
            String.format("Left Max Angle: %.1f°", IntakeConstants.AUTO_INTAKE_TURRET_LEFT_MAX_ANGLE),
            String.format("Right Max Angle: %.1f°", IntakeConstants.AUTO_INTAKE_TURRET_RIGHT_MAX_ANGLE),
            String.format("Safety Margin: %.1f\"", IntakeConstants.STADIUM_SAFETY_MARGIN),
            String.format("Area Check: %s", IntakeConstants.ENABLE_STADIUM_AREA_CHECK ? "ENABLED" : "DISABLED")
        };
    }
    
    /**
     * Validate stadium area configuration
     * @return true if configuration is valid
     */
    public static boolean validateConfiguration() {
        // Check that slide length and shoulder radius are positive
        if (IntakeConstants.SLIDE_LENGTH_INCHES <= 0 || IntakeConstants.SHOULDER_RADIUS_INCHES <= 0) {
            return false;
        }
        
        // Check that max reach is reasonable
        double expectedMaxReach = IntakeConstants.SLIDE_LENGTH_INCHES + IntakeConstants.SHOULDER_RADIUS_INCHES;
        if (Math.abs(IntakeConstants.AUTO_INTAKE_MAX_SLIDE_REACH - expectedMaxReach) > 2.0) {
            return false; // More than 2" difference is suspicious
        }
        
        // Check that safety margin is reasonable
        if (IntakeConstants.STADIUM_SAFETY_MARGIN < 0 || IntakeConstants.STADIUM_SAFETY_MARGIN > 5.0) {
            return false;
        }
        
        return true;
    }
}
