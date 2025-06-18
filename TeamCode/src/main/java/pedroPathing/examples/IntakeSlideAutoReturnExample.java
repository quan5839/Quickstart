package pedroPathing.examples;

import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import pedroPathing.BaseTeleop25152;
import pedroPathing.constants.IntakeConstants;
import pedroPathing.robot_state.RobotMode;
import pedroPathing.util.TeamColor;

/**
 * Example demonstrating the new intake slide auto-return functionality
 * 
 * Controls:
 * - Gamepad1 A: Move slide to HOLD position with auto-return (recommended method)
 * - Gamepad1 B: Move slide to HOLD position with smart state machine integration
 * - Gamepad1 X: Move slide to HOLD position without auto-return (old behavior)
 * - Gamepad1 Y: Move slide to MAX position (no auto-return)
 * - Gamepad1 Right Bumper: Manually move slide to MIN position
 * 
 * Watch the telemetry to see the slide position and auto-return behavior.
 */
@TeleOp(name = "Intake Slide Auto-Return Example", group = "Examples")
@Disabled // Remove this line to enable the example
public class IntakeSlideAutoReturnExample extends BaseTeleop25152 {

    @Override
    protected RobotMode getRobotMode() {
        return RobotMode.SAMPLE; // Use sample mode for this example
    }

    @Override
    protected TeamColor getTeamColor() {
        return TeamColor.BLUE; // Default team color for example
    }

    @Override
    public void loop() {
        // Call parent loop to handle all the standard functionality
        super.loop();

        // Handle example-specific controls
        handleExampleControls();
        
        // Display example telemetry
        displayExampleTelemetry();
    }

    /**
     * Handle the example-specific gamepad controls
     */
    private void handleExampleControls() {
        // Method 1: Auto-return with thread-based approach (works everywhere)
        if (gamepad1.a && !gamepad1.start) { // Avoid conflict with standard controls
            robot.intake.setIntakeSlidePositionWithAutoReturn(IntakeConstants.SLIDE_HOLD);
            telemetry.addData("Action", "Slide to HOLD with auto-return (thread-based)");
        }

        // Method 2: Smart positioning with state machine integration (optimal for teleop)
        if (gamepad1.b && !gamepad1.start) {
            robot.intake.setIntakeSlidePositionSmart(IntakeConstants.SLIDE_HOLD);
            telemetry.addData("Action", "Slide to HOLD with smart positioning");
        }

        // Method 3: Traditional positioning without auto-return (old behavior)
        if (gamepad1.x && !gamepad1.start) {
            robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_HOLD);
            telemetry.addData("Action", "Slide to HOLD (no auto-return)");
        }

        // Method 4: Move to MAX position (no auto-return expected)
        if (gamepad1.y && !gamepad1.start) {
            robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MAX);
            telemetry.addData("Action", "Slide to MAX position");
        }

        // Manual control: Move to MIN position
        if (gamepad1.right_bumper) {
            robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MIN);
            telemetry.addData("Action", "Manual slide to MIN position");
        }

        // Example of conditional auto-return
        if (gamepad1.left_bumper && !gamepad1.start) {
            // Only enable auto-return if we're in a specific state
            boolean enableAutoReturn = getStateMachine().getCurrentState().toString().contains("INTAKE");
            robot.intake.setIntakeSlidePositionSmart(IntakeConstants.SLIDE_HOLD, enableAutoReturn);
            telemetry.addData("Action", "Conditional auto-return: " + enableAutoReturn);
        }
    }

    /**
     * Display telemetry specific to this example
     */
    private void displayExampleTelemetry() {
        telemetry.addLine("=== INTAKE SLIDE AUTO-RETURN EXAMPLE ===");
        telemetry.addLine();
        
        // Current slide position
        double currentPosition = robot.intake.getIntakeSlidePosition();
        telemetry.addData("Current Slide Position", String.format("%.3f", currentPosition));
        
        // Position interpretation
        String positionName = "UNKNOWN";
        if (Math.abs(currentPosition - IntakeConstants.SLIDE_MIN) < 0.05) {
            positionName = "MIN (Retracted)";
        } else if (Math.abs(currentPosition - IntakeConstants.SLIDE_HOLD) < 0.05) {
            positionName = "HOLD";
        } else if (Math.abs(currentPosition - IntakeConstants.SLIDE_MAX) < 0.05) {
            positionName = "MAX (Extended)";
        } else if (Math.abs(currentPosition - IntakeConstants.SLIDE_RELEASE) < 0.05) {
            positionName = "RELEASE";
        }
        telemetry.addData("Position Name", positionName);
        
        // Limit switch status
        boolean atHoldPosition = robot.intake.isSlideAtHoldPosition();
        boolean atMinPosition = robot.intake.isSlideAtMinPosition();
        telemetry.addData("At Hold Position", atHoldPosition);
        telemetry.addData("At Min Position", atMinPosition);
        
        // State machine info
        telemetry.addData("Current State", getStateMachine().getCurrentState());
        telemetry.addData("Current Mode", getStateMachine().getCurrentMode());
        
        telemetry.addLine();
        telemetry.addLine("=== CONTROLS ===");
        telemetry.addLine("A: HOLD with auto-return (thread)");
        telemetry.addLine("B: HOLD with smart positioning");
        telemetry.addLine("X: HOLD without auto-return");
        telemetry.addLine("Y: MAX position");
        telemetry.addLine("RB: Manual MIN position");
        telemetry.addLine("LB: Conditional auto-return");
        
        telemetry.addLine();
        telemetry.addData("Auto-Return Delay", IntakeConstants.SLIDE_AUTO_RETURN_DELAY + "ms");
    }
}
