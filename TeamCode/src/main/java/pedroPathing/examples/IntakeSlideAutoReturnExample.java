//package pedroPathing.examples;
//
//import com.qualcomm.robotcore.eventloop.opmode.Disabled;
//import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
//import pedroPathing.BaseTeleop25152;
//import pedroPathing.constants.IntakeConstants;
//import pedroPathing.robot_state.RobotMode;
//
///**
// * Example demonstrating the new intake slide auto-return functionality
// *
// * Controls:
// * - Gamepad1 A: Move slide to HOLD position with auto-return (new method)
// * - Gamepad1 X: Move slide to HOLD position without auto-return (old behavior)
// * - Gamepad1 Y: Move slide to MAX position (no auto-return)
// * - Gamepad1 Right Bumper: Manually move slide to MIN position
// *
// * Watch the telemetry to see the slide position and auto-return behavior.
// */
//@TeleOp(name = "Intake Slide Auto-Return Example", group = "Examples")
//@Disabled // Remove this line to enable the example
//public class IntakeSlideAutoReturnExample extends BaseTeleop25152 {
//
//    @Override
//    protected RobotMode getRobotMode() {
//        return RobotMode.SAMPLE; // Use sample mode for this example
//    }
//
//    @Override
//    protected BaseTeleop25152.TeamColor getTeamColor() {
//        return BaseTeleop25152.TeamColor.BLUE; // Default team color for example
//    }
//
//    @Override
//    public void loop() {
//        // Call parent loop to handle all the standard functionality
//        super.loop();
//
//        // Handle example-specific controls
//        handleExampleControls();
//
//        // Display example telemetry
//        displayExampleTelemetry();
//    }
//
//    /**
//     * Handle the example-specific gamepad controls
//     */
//    private void handleExampleControls() {
//        // Method 1: Auto-return positioning (new method)
//        if (gamepad1.a && !gamepad1.start) { // Avoid conflict with standard controls
//            getRobot().intake.setIntakeSlidePositionWithAutoReturn(IntakeConstants.SLIDE_HOLD);
//            telemetry.addData("Action", "Slide to HOLD with auto-return");
//        }
//
//        // Method 2: Traditional positioning without auto-return (old behavior)
//        if (gamepad1.x && !gamepad1.start) {
//            getRobot().intake.setIntakeSlidePosition(IntakeConstants.SLIDE_HOLD);
//            telemetry.addData("Action", "Slide to HOLD (no auto-return)");
//        }
//
//        // Method 3: Move to MAX position (no auto-return expected)
//        if (gamepad1.y && !gamepad1.start) {
//            getRobot().intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MAX);
//            telemetry.addData("Action", "Slide to MAX position");
//        }
//
//        // Manual control: Move to MIN position
//        if (gamepad1.right_bumper) {
//            getRobot().intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MIN);
//            telemetry.addData("Action", "Manual slide to MIN position");
//        }
//    }
//
//    /**
//     * Display telemetry specific to this example
//     */
//    private void displayExampleTelemetry() {
//        telemetry.addLine("=== INTAKE SLIDE AUTO-RETURN EXAMPLE ===");
//        telemetry.addLine();
//
//        // Current slide position
//        double currentPosition = getRobot().intake.getIntakeSlidePosition();
//        telemetry.addData("Current Slide Position", String.format("%.3f", currentPosition));
//
//        // Position interpretation
//        String positionName = "UNKNOWN";
//        if (Math.abs(currentPosition - IntakeConstants.SLIDE_MIN) < 0.05) {
//            positionName = "MIN (Retracted)";
//        } else if (Math.abs(currentPosition - IntakeConstants.SLIDE_HOLD) < 0.05) {
//            positionName = "HOLD";
//        } else if (Math.abs(currentPosition - IntakeConstants.SLIDE_MAX) < 0.05) {
//            positionName = "MAX (Extended)";
//        } else if (Math.abs(currentPosition - IntakeConstants.SLIDE_RELEASE) < 0.05) {
//            positionName = "RELEASE";
//        }
//        telemetry.addData("Position Name", positionName);
//
//        // Limit switch status
//        boolean atHoldPosition = getRobot().intake.isSlideAtHoldPosition();
//        boolean atMinPosition = getRobot().intake.isSlideAtMinPosition();
//        telemetry.addData("At Hold Position", atHoldPosition);
//        telemetry.addData("At Min Position", atMinPosition);
//
//        // State machine info
//        telemetry.addData("Current State", getStateMachine().getCurrentState());
//        telemetry.addData("Current Mode", getStateMachine().getCurrentMode());
//
//        telemetry.addLine();
//        telemetry.addLine("=== CONTROLS ===");
//        telemetry.addLine("A: HOLD with auto-return (new)");
//        telemetry.addLine("X: HOLD without auto-return (old)");
//        telemetry.addLine("Y: MAX position");
//        telemetry.addLine("RB: Manual MIN position");
//
//        telemetry.addLine();
//        telemetry.addData("Auto-Return Delay", IntakeConstants.SLIDE_AUTO_RETURN_DELAY + "ms");
//    }
//}
