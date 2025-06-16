package pedroPathing.hardware;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import pedroPathing.constants.OuttakeConstants;
import pedroPathing.util.CustomPIDFController;

/**
 * Subsystem for controlling the outtake slide mechanism with custom PIDF control
 * Provides precise position control and better debugging capabilities
 */
public class OuttakeSlideSubsystem {
    // Motors
    private DcMotorEx outtakeSlideLeft;
    private DcMotorEx outtakeSlideRight;

    // Custom PIDF controllers for precise control
    private CustomPIDFController leftController;
    private CustomPIDFController rightController;

    // Conversion factor from degrees to encoder ticks
    private final double TICKS_PER_DEGREE = 384.5 / 360.0;

    // Telemetry for debugging
    private Telemetry telemetry;

    // Control state
    private boolean controllersEnabled = true;
    private double targetPosition = 0;

    /**
     * Constructor for OuttakeSlideSubsystem with custom PIDF control
     *
     * @param hardwareMap Hardware map from OpMode
     * @param telemetry Telemetry from OpMode
     */
    public OuttakeSlideSubsystem(HardwareMap hardwareMap, Telemetry telemetry) {
        // Initialize motors using helper
        outtakeSlideRight = initializeMotor(hardwareMap, "outtakeSlideRight", DcMotorSimple.Direction.FORWARD);
        outtakeSlideLeft = initializeMotor(hardwareMap, "outtakeSlideLeft", DcMotorSimple.Direction.REVERSE);

        // Initialize custom PIDF controllers
        leftController = new CustomPIDFController(
            OuttakeConstants.SLIDE_P,
            OuttakeConstants.SLIDE_I,
            OuttakeConstants.SLIDE_D,
            OuttakeConstants.SLIDE_F
        );
        rightController = new CustomPIDFController(
            OuttakeConstants.SLIDE_P,
            OuttakeConstants.SLIDE_I,
            OuttakeConstants.SLIDE_D,
            OuttakeConstants.SLIDE_F
        );

        // Configure controller tolerances
        leftController.setPositionTolerance(15);
        rightController.setPositionTolerance(15);
        leftController.setVelocityTolerance(10);
        rightController.setVelocityTolerance(10);

        // Set reasonable output limits
        leftController.setOutputLimits(-1.0, 1.0);
        rightController.setOutputLimits(-1.0, 1.0);

        // Enable integral windup prevention
        leftController.setIntegralLimit(0.5);
        rightController.setIntegralLimit(0.5);

        // Set up telemetry with FTC Dashboard
        this.telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
    }

    /**
     * Helper to initialize a motor for custom PIDF control
     * Motors are set to RUN_WITHOUT_ENCODER since we're using custom control
     */
    private DcMotorEx initializeMotor(HardwareMap hardwareMap, String name, DcMotorSimple.Direction direction) {
        DcMotorEx motor = hardwareMap.get(DcMotorEx.class, name);
        motor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        motor.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER); // Custom PIDF control
        motor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motor.setDirection(direction);
        motor.setPower(0); // Start with zero power
        return motor;
    }

    /**
     * Initialize the subsystem
     */
    public void init() {
        resetEncoders();
        setTargetPosition(0);
        telemetry.addData("Status", "OuttakeSlide initialized successfully with custom PIDF");
        telemetry.update();
    }

    /**
     * Reset the slide encoders and controllers
     */
    public void resetEncoders() {
        outtakeSlideLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        outtakeSlideRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        outtakeSlideLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        outtakeSlideRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        // Reset custom controllers
        leftController.reset();
        rightController.reset();
        leftController.setTarget(0);
        rightController.setTarget(0);
        targetPosition = 0;
    }

    /**
     * Set the target position for the slide using custom PIDF control
     *
     * @param position Target position in encoder ticks
     */
    public void setTargetPosition(int position) {
        targetPosition = position;
        OuttakeConstants.SLIDE_TARGET_POSITION = position;

        if (controllersEnabled) {
            leftController.setTarget(position);
            rightController.setTarget(position);
        }
    }

    /**
     * Set the target position in degrees
     *
     * @param degrees Target angle in degrees
     */
    public void setTargetPositionDegrees(double degrees) {
        int ticks = (int)(degrees * TICKS_PER_DEGREE);
        setTargetPosition(ticks);
    }

    /**
     * Enable or disable custom PIDF controllers
     * When disabled, motors can be controlled manually
     * @param enabled true to enable custom control, false to disable
     */
    public void setControllersEnabled(boolean enabled) {
        this.controllersEnabled = enabled;
        if (!enabled) {
            // Stop motors when disabling controllers
            outtakeSlideLeft.setPower(0);
            outtakeSlideRight.setPower(0);
        }
    }

    /**
     * Update PIDF coefficients for both controllers
     * @param kP Proportional gain
     * @param kI Integral gain
     * @param kD Derivative gain
     * @param kF Feed-forward gain
     */
    public void updatePIDFCoefficients(double kP, double kI, double kD, double kF) {
        leftController.setPIDF(kP, kI, kD, kF);
        rightController.setPIDF(kP, kI, kD, kF);
    }

    /**
     * Update the custom PIDF control and telemetry
     * Should be called regularly in the OpMode loop
     */
    public void update() {
        int leftPosition = outtakeSlideLeft.getCurrentPosition();
        int rightPosition = outtakeSlideRight.getCurrentPosition();
        int currentPosition = (leftPosition + rightPosition) / 2;
        double targetAngleDegrees = targetPosition / TICKS_PER_DEGREE;

        if (controllersEnabled) {
            // Update PIDF coefficients from constants (allows live tuning)
            updatePIDFCoefficients(
                OuttakeConstants.SLIDE_P,
                OuttakeConstants.SLIDE_I,
                OuttakeConstants.SLIDE_D,
                OuttakeConstants.SLIDE_F
            );

            // Calculate control outputs
            double leftPower = leftController.calculate(leftPosition);
            double rightPower = rightController.calculate(rightPosition);

            // Apply power to motors
            outtakeSlideLeft.setPower(leftPower);
            outtakeSlideRight.setPower(rightPower);
        }

        // Telemetry
        telemetry.addData("Outtake Target", (int)targetPosition);
        telemetry.addData("Target Angle (deg)", String.format("%.2f", targetAngleDegrees));
        telemetry.addData("Outtake Current", currentPosition);
        telemetry.addData("Left Position", leftPosition);
        telemetry.addData("Right Position", rightPosition);
        telemetry.addData("Controllers Enabled", controllersEnabled);

        if (controllersEnabled) {
            telemetry.addData("Left At Target", leftController.atTarget(leftPosition));
            telemetry.addData("Right At Target", rightController.atTarget(rightPosition));
            telemetry.addData("Left Power", String.format("%.3f", outtakeSlideLeft.getPower()));
            telemetry.addData("Right Power", String.format("%.3f", outtakeSlideRight.getPower()));
            telemetry.addData("Left Debug", leftController.getDebugInfo(leftPosition));
            telemetry.addData("Right Debug", rightController.getDebugInfo(rightPosition));
        }
    }

    /**
     * Stop the slide motors and reset controllers
     */
    public void stop() {
        outtakeSlideLeft.setPower(0);
        outtakeSlideRight.setPower(0);

        if (controllersEnabled) {
            leftController.reset();
            rightController.reset();
        }
    }

    /**
     * Get the left slide position
     */
    public int getLeftPosition() {
        return -outtakeSlideLeft.getCurrentPosition();
    }

    /**
     * Get the right slide position
     */
    public int getRightPosition() {
        return outtakeSlideRight.getCurrentPosition();
    }

    /**
     * Get the average slide position
     */
    public int getCurrentPosition() {
        return (-getLeftPosition() + getRightPosition()) / 2;
    }

    /**
     * Check if both controllers are at their target positions
     * @return true if both slides are at target
     */
    public boolean atTarget() {
        if (!controllersEnabled) {
            return false; // Can't determine target status without controllers
        }

        int leftPosition = outtakeSlideLeft.getCurrentPosition();
        int rightPosition = outtakeSlideRight.getCurrentPosition();

        return leftController.atTarget(leftPosition) && rightController.atTarget(rightPosition);
    }

    /**
     * Get the current target position
     * @return Target position in encoder ticks
     */
    public double getTargetPosition() {
        return targetPosition;
    }

    /**
     * Get the position error for debugging
     * @return Average position error in encoder ticks
     */
    public double getPositionError() {
        if (!controllersEnabled) {
            return 0;
        }

        int leftPosition = outtakeSlideLeft.getCurrentPosition();
        int rightPosition = outtakeSlideRight.getCurrentPosition();

        double leftError = leftController.getError(leftPosition);
        double rightError = rightController.getError(rightPosition);

        return (leftError + rightError) / 2.0;
    }

    /**
     * Get the current velocity for debugging
     * @return Average velocity in ticks per second
     */
    public double getVelocity() {
        if (!controllersEnabled) {
            return 0;
        }

        return (leftController.getVelocity() + rightController.getVelocity()) / 2.0;
    }

    /**
     * Set manual power to motors (disables controllers temporarily)
     * Use this for manual control or emergency override
     * @param power Power value (-1.0 to 1.0)
     */
    public void setManualPower(double power) {
        setControllersEnabled(false);
        outtakeSlideLeft.setPower(power);
        outtakeSlideRight.setPower(power);
    }

    /**
     * Get comprehensive debug information
     * @return Formatted debug string
     */
    public String getDebugInfo() {
        if (!controllersEnabled) {
            return "Controllers disabled - Manual control active";
        }

        int leftPos = outtakeSlideLeft.getCurrentPosition();
        int rightPos = outtakeSlideRight.getCurrentPosition();

        return String.format("Target: %.0f | Avg: %.0f | AtTarget: %s | Error: %.1f | Vel: %.1f",
                           targetPosition,
                           (leftPos + rightPos) / 2.0,
                           atTarget(),
                           getPositionError(),
                           getVelocity());
    }
}
