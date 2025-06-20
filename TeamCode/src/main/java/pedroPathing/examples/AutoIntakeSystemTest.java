package pedroPathing.examples;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;

import pedroPathing.BaseTeleop25152;
import pedroPathing.constants.IntakeConstants;
import pedroPathing.hardware.AutoIntakeSystem;
import pedroPathing.robot_state.RobotMode;

/**
 * AutoIntakeSystem Test Teleop
 * 
 * This teleop demonstrates and tests the AutoIntakeSystem with stadium area calculations.
 * It provides manual controls for testing the full slide and turret positioning system
 * and validates the stadium-shaped reachable area constraints.
 * 
 * Stadium Area Description:
 * - Forward semicircle: slide extended + shoulder rotation
 * - Rectangle sides: slide movement with turret rotation
 * - Constraints: 40cm slide length, 17cm shoulder radius
 * 
 * Controls:
 * - Gamepad1 A: Start AutoIntakeSystem sequence
 * - Gamepad1 B: Stop AutoIntakeSystem
 * - Gamepad1 X: Test stadium area at current position
 * - Gamepad1 Y: Toggle vision color recognition
 * - Left Stick: Manual target position (X/Y)
 * - Right Stick: Manual turret/slide control
 * - D-pad: Preset stadium test positions
 * 
 * @author Baron Henderson - 20077 The Indubitables
 * @version 1.0, 12/30/2024
 */
@TeleOp(name = "AutoIntakeSystem Test", group = "Examples")
@Disabled // Remove this line to enable the test
public class AutoIntakeSystemTest extends BaseTeleop25152 {

    // AutoIntakeSystem instance
    private AutoIntakeSystem autoIntakeSystem;
    
    // Manual testing variables
    private double manualTargetX = 0.0;
    private double manualTargetY = 0.0;
    private boolean isManualMode = false;
    private ElapsedTime testTimer = new ElapsedTime();
    
    // Stadium area test positions (robot-relative coordinates in inches)
    private final double[][] STADIUM_TEST_POSITIONS = {
        {12.0, 0.0},    // Forward center
        {10.0, 8.0},    // Forward right
        {10.0, -8.0},   // Forward left
        {6.0, 12.0},    // Right side
        {6.0, -12.0},   // Left side
        {0.0, 15.0},    // Far right
        {0.0, -15.0},   // Far left
        {-2.0, 10.0},   // Behind right (should fail)
        {-2.0, -10.0},  // Behind left (should fail)
        {20.0, 0.0},    // Too far forward (should fail)
    };
    private int currentTestPosition = 0;

    @Override
    protected RobotMode getRobotMode() {
        return RobotMode.SAMPLE; // Use sample mode for intake testing
    }

    @Override
    protected BaseTeleop25152.TeamColor getTeamColor() {
        return BaseTeleop25152.TeamColor.BLUE; // Default team color for testing
    }

    @Override
    public void init() {
        // Call parent initialization
        super.init();
        
        // Initialize AutoIntakeSystem
        autoIntakeSystem = new AutoIntakeSystem(
            getRobot(), 
            getRobot().visionSystem, 
            getStateMachine(), 
            this
        );
        
        telemetry.addData("Status", "AutoIntakeSystem Test Initialized");
        telemetry.addData("Stadium Area Check", IntakeConstants.ENABLE_STADIUM_AREA_CHECK ? "ENABLED" : "DISABLED");
        telemetry.update();
    }

    @Override
    public void loop() {
        // Call parent loop to handle all standard functionality
        super.loop();
        
        // Update AutoIntakeSystem
        autoIntakeSystem.update();
        
        // Handle test-specific controls
        handleTestControls();
        
        // Handle manual positioning
        handleManualPositioning();
        
        // Display test telemetry
        displayTestTelemetry();
    }

    /**
     * Handle test-specific gamepad controls
     */
    private void handleTestControls() {
        // Start AutoIntakeSystem sequence
        if (gamepad1.a && !gamepad1.start) {
            if (autoIntakeSystem.startAutoIntake()) {
                telemetry.addData("Action", "✅ AutoIntake Started");
            } else {
                telemetry.addData("Action", "❌ AutoIntake Failed to Start");
            }
        }
        
        // Stop AutoIntakeSystem
        if (gamepad1.b && !gamepad1.start) {
            autoIntakeSystem.stopAutoIntake();
            telemetry.addData("Action", "⏹️ AutoIntake Stopped");
        }
        
        // Test stadium area at current manual position
        if (gamepad1.x && !gamepad1.start) {
            testStadiumAreaAtPosition(manualTargetX, manualTargetY);
        }
        
        // Toggle vision color recognition
        if (gamepad1.y && !gamepad1.start) {
            boolean currentMode = getRobot().visionSystem.isColorRecognitionEnabled();
            getRobot().visionSystem.setColorRecognitionEnabled(!currentMode);
            telemetry.addData("Vision Color Recognition", !currentMode ? "ENABLED" : "DISABLED");
        }
        
        // Cycle through preset stadium test positions
        if (gamepad1.dpad_up) {
            currentTestPosition = (currentTestPosition + 1) % STADIUM_TEST_POSITIONS.length;
            manualTargetX = STADIUM_TEST_POSITIONS[currentTestPosition][0];
            manualTargetY = STADIUM_TEST_POSITIONS[currentTestPosition][1];
            testStadiumAreaAtPosition(manualTargetX, manualTargetY);
        }
        
        if (gamepad1.dpad_down) {
            currentTestPosition = (currentTestPosition - 1 + STADIUM_TEST_POSITIONS.length) % STADIUM_TEST_POSITIONS.length;
            manualTargetX = STADIUM_TEST_POSITIONS[currentTestPosition][0];
            manualTargetY = STADIUM_TEST_POSITIONS[currentTestPosition][1];
            testStadiumAreaAtPosition(manualTargetX, manualTargetY);
        }
        
        // Reset to center position
        if (gamepad1.dpad_left || gamepad1.dpad_right) {
            manualTargetX = 0.0;
            manualTargetY = 0.0;
            currentTestPosition = 0;
        }
    }

    /**
     * Handle manual positioning controls
     */
    private void handleManualPositioning() {
        // Manual target position control with left stick
        if (Math.abs(gamepad1.left_stick_x) > 0.1 || Math.abs(gamepad1.left_stick_y) > 0.1) {
            isManualMode = true;
            manualTargetX += gamepad1.left_stick_y * 0.5; // Forward/backward
            manualTargetY += gamepad1.left_stick_x * 0.5; // Left/right
            
            // Clamp to reasonable bounds
            manualTargetX = Math.max(-5.0, Math.min(25.0, manualTargetX));
            manualTargetY = Math.max(-20.0, Math.min(20.0, manualTargetY));
        }
        
        // Manual turret and slide control with right stick
        if (Math.abs(gamepad1.right_stick_x) > 0.1 || Math.abs(gamepad1.right_stick_y) > 0.1) {
            // Right stick Y: slide position
            double slideInput = -gamepad1.right_stick_y; // Invert for intuitive control
            double slidePosition = IntakeConstants.SLIDE_MIN + 
                (slideInput + 1.0) / 2.0 * (IntakeConstants.SLIDE_MAX - IntakeConstants.SLIDE_MIN);
            getRobot().intake.setIntakeSlidePosition(slidePosition);
            
            // Right stick X: turret position
            double turretInput = gamepad1.right_stick_x;
            double turretPosition = IntakeConstants.TURRET_MIDDLE + 
                turretInput * (IntakeConstants.TURRET_LEFT - IntakeConstants.TURRET_RIGHT) / 2.0;
            getRobot().intake.setIntakeTurretPosition(turretPosition);
            
            telemetry.addData("Manual Control", "Slide: %.3f, Turret: %.3f", slidePosition, turretPosition);
        }
    }

    /**
     * Test stadium area reachability at a specific position
     */
    private void testStadiumAreaAtPosition(double x, double y) {
        boolean isReachable = isPositionInStadiumArea(x, y);
        double distance = Math.sqrt(x * x + y * y);
        double angle = Math.toDegrees(Math.atan2(y, x));
        
        telemetry.addData("Stadium Test", "Position: (%.1f, %.1f)", x, y);
        telemetry.addData("Stadium Result", isReachable ? "✅ REACHABLE" : "❌ OUT OF REACH");
        telemetry.addData("Distance", "%.1f inches", distance);
        telemetry.addData("Angle", "%.1f degrees", angle);
        
        if (isReachable) {
            // Calculate required servo positions
            double[] servoPositions = calculateServoPositionsForPosition(x, y);
            telemetry.addData("Turret Position", "%.3f", servoPositions[0]);
            telemetry.addData("Slide Position", "%.3f", servoPositions[1]);
        }
    }

    /**
     * Check if a position is within the stadium-shaped reachable area
     */
    private boolean isPositionInStadiumArea(double x, double y) {
        if (!IntakeConstants.ENABLE_STADIUM_AREA_CHECK) {
            return true; // Skip check if disabled
        }
        
        double distance = Math.sqrt(x * x + y * y);
        double angle = Math.atan2(y, x);
        
        // Check maximum reach distance
        if (distance > IntakeConstants.AUTO_INTAKE_MAX_REACH) {
            return false;
        }
        
        // Check minimum distance
        if (distance < IntakeConstants.AUTO_INTAKE_MIN_DISTANCE) {
            return false;
        }
        
        // Check angle limits (forward-facing intake only)
        double angleDegrees = Math.toDegrees(angle);
        if (Math.abs(angleDegrees) > IntakeConstants.AUTO_INTAKE_MAX_ANGLE) {
            return false;
        }
        
        // Stadium area calculation:
        // Forward semicircle: x > 0 and within slide + shoulder reach
        // Rectangle sides: x <= shoulder_radius and within slide reach
        
        if (x > IntakeConstants.SHOULDER_RADIUS_INCHES) {
            // Forward semicircle area
            double maxReachAtAngle = IntakeConstants.SLIDE_LENGTH_INCHES + IntakeConstants.SHOULDER_RADIUS_INCHES;
            return distance <= (maxReachAtAngle - IntakeConstants.STADIUM_SAFETY_MARGIN);
        } else {
            // Rectangle sides area
            double maxSideReach = IntakeConstants.SLIDE_LENGTH_INCHES;
            return Math.abs(y) <= (maxSideReach - IntakeConstants.STADIUM_SAFETY_MARGIN) && x >= -IntakeConstants.STADIUM_SAFETY_MARGIN;
        }
    }

    /**
     * Calculate servo positions for a given target position
     * Returns [turretPosition, slidePosition]
     */
    private double[] calculateServoPositionsForPosition(double x, double y) {
        double distance = Math.sqrt(x * x + y * y);
        double angle = Math.atan2(y, x);
        
        // Calculate turret position
        double angleDegrees = Math.toDegrees(angle);
        double turretRange = IntakeConstants.TURRET_LEFT - IntakeConstants.TURRET_RIGHT;
        double normalizedAngle = angleDegrees / IntakeConstants.AUTO_INTAKE_MAX_ANGLE;
        double turretPosition = IntakeConstants.TURRET_MIDDLE + (normalizedAngle * turretRange / 2.0);
        turretPosition = Math.max(IntakeConstants.TURRET_RIGHT, Math.min(turretPosition, IntakeConstants.TURRET_LEFT));
        
        // Calculate slide position
        double slideExtension = Math.min(distance / IntakeConstants.AUTO_INTAKE_MAX_REACH, 1.0);
        double slidePosition = IntakeConstants.SLIDE_MIN - (slideExtension * (IntakeConstants.SLIDE_MIN - IntakeConstants.SLIDE_MAX));
        
        return new double[]{turretPosition, slidePosition};
    }

    /**
     * Display comprehensive test telemetry
     */
    private void displayTestTelemetry() {
        telemetry.addLine("=== AUTO INTAKE SYSTEM TEST ===");
        telemetry.addLine();
        
        // AutoIntakeSystem status
        telemetry.addData("Auto Intake Active", autoIntakeSystem.isActive());
        telemetry.addData("Auto Intake State", autoIntakeSystem.getCurrentState());
        
        if (autoIntakeSystem.isActive()) {
            telemetry.addData("Target Position", String.format("X:%.1f Y:%.1f", 
                autoIntakeSystem.getTargetX(), autoIntakeSystem.getTargetY()));
            telemetry.addData("Target Distance", String.format("%.1f inches", autoIntakeSystem.getTargetDistance()));
        }
        
        // Manual positioning
        telemetry.addLine();
        telemetry.addData("Manual Target", String.format("X:%.1f Y:%.1f", manualTargetX, manualTargetY));
        telemetry.addData("Test Position", String.format("%d/%d", currentTestPosition + 1, STADIUM_TEST_POSITIONS.length));
        
        // Current servo positions
        telemetry.addLine();
        telemetry.addData("Current Slide", String.format("%.3f", getRobot().intake.getIntakeSlidePosition()));
        telemetry.addData("Current Turret", String.format("%.3f", getRobot().intake.getIntakeTurretPosition()));
        
        // Vision system status
        telemetry.addLine();
        telemetry.addData("Vision Target", getRobot().visionSystem.isTargetVisible() ? "VISIBLE" : "NOT VISIBLE");
        telemetry.addData("Color Recognition", getRobot().visionSystem.isColorRecognitionEnabled() ? "ON" : "OFF");
        telemetry.addData("Sample Detected", getRobot().visionSystem.isColorSampleDetected() ? "YES" : "NO");
        
        // Stadium area constants
        telemetry.addLine();
        telemetry.addData("Slide Length", String.format("%.1f inches", IntakeConstants.SLIDE_LENGTH_INCHES));
        telemetry.addData("Shoulder Radius", String.format("%.1f inches", IntakeConstants.SHOULDER_RADIUS_INCHES));
        telemetry.addData("Max Reach", String.format("%.1f inches", IntakeConstants.AUTO_INTAKE_MAX_REACH));
        telemetry.addData("Max Angle", String.format("%.1f degrees", IntakeConstants.AUTO_INTAKE_MAX_ANGLE));
        
        telemetry.addLine();
        telemetry.addLine("=== CONTROLS ===");
        telemetry.addLine("A: Start Auto Intake");
        telemetry.addLine("B: Stop Auto Intake");
        telemetry.addLine("X: Test Stadium Area");
        telemetry.addLine("Y: Toggle Vision");
        telemetry.addLine("D-pad: Cycle Test Positions");
        telemetry.addLine("Left Stick: Manual Target");
        telemetry.addLine("Right Stick: Manual Servos");
    }
}
