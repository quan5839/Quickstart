package pedroPathing;

import android.graphics.Color;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.pedropathing.follower.Follower;
import com.pedropathing.localization.Pose;
import com.pedropathing.util.Constants;
import com.qualcomm.hardware.bosch.BHI260IMU;
import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;

import java.util.List;
import java.util.Locale;

import pedroPathing.constants.ControlConstants;
import pedroPathing.constants.FConstants;
import pedroPathing.constants.IntakeConstants;
import pedroPathing.constants.LConstants;
import pedroPathing.hardware.DriveSystem.Orientation;
import pedroPathing.hardware.RobotHardware;
import pedroPathing.hardware.SampleColor;
import pedroPathing.robot_state.RobotMode;
import pedroPathing.robot_state.RobotStateMachine;
import pedroPathing.util.ButtonEdgeDetector;
import pedroPathing.util.PerformanceMonitor;

/**
 * Base teleop class that contains all common functionality for the four teleop variants.
 * This eliminates code duplication while maintaining separate files for competition use.
 *
 * @author Baron Henderson - 20077 The Indubitables
 * @version 2.0, 12/30/2024
 */
public abstract class BaseTeleop25152 extends OpMode {

    //TODO: Export and Import robot config file
    // adb pull /sdcard/FIRST/config.xml
    // adb push config.xml /sdcard/FIRST/config.xml

    private Follower follower;
    private final Pose startPose = new Pose(0, 0, 0);
    private final RobotHardware robot = new RobotHardware(this);
    private RobotStateMachine stateMachine;

    // FTC Dashboard for web telemetry
    private FtcDashboard dashboard;
    private TelemetryPacket packet;

    private long lastUpdateTime = System.currentTimeMillis();

    // IMU support - Pinpoint with REV fallback
    private Object imu; // Will be either GoBildaPinpointDriver or BHI260IMU
    private boolean usingPinpointIMU = false;

    // Color recognition control
    private boolean colorRecognitionMode = false;

    // Enhanced loop time tracking with comprehensive metrics
    private final PerformanceMonitor performanceMonitor = new PerformanceMonitor();

    // Team color management for FTC competition
    public enum TeamColor {
        RED(Color.RED, "Red Alliance"),
        BLUE(Color.BLUE, "Blue Alliance");

        public final int hubColor;
        public final String displayName;

        TeamColor(int hubColor, String displayName) {
            this.hubColor = hubColor;
            this.displayName = displayName;
        }
    }

    // Enhanced sample detection and feedback
    private SampleColor lastDetectedSample = SampleColor.NONE;
    private long lastSampleFeedbackTime = 0;
    private boolean isBlinkingForSample = false;

    // Advanced rumble patterns
    private int rumbleSequenceStep = 0;
    private long lastRumbleTime = 0;
    private boolean isRumbleSequenceActive = false;

    // Performance optimization: Cache expensive hardware queries
    private List<LynxModule> cachedHubs;
    private long lastColorToggleTime = 0;
    private boolean lastColorToggleState = false;
    private long lastTelemetryUpdate = 0;

    // Performance optimization: Pre-allocated StringBuilder for telemetry
    private final StringBuilder telemetryBuilder = new StringBuilder(ControlConstants.TELEMETRY_BUILDER_CAPACITY);

    // Shared edge detection for gamepad buttons
    private ButtonEdgeDetector buttonDetectorDriver = new ButtonEdgeDetector();
    private ButtonEdgeDetector buttonDetectorIntakeOuttake = new ButtonEdgeDetector();

    /**
     * Abstract methods that derived classes must implement to specify their configuration
     */
    protected abstract TeamColor getTeamColor();
    protected abstract RobotMode getRobotMode();

    /**
     * This method is called once when init is played, it initializes the follower and state machine
     **/
    @Override
    public void init() {
        // Check for pose transfer from AUTO_POSE SharedPreferences
        android.content.SharedPreferences prefs = hardwareMap.appContext.getSharedPreferences("AUTO_POSE", 0);
        boolean hasPose = prefs.contains("x") && prefs.contains("y") && prefs.contains("heading");
        Pose teleopStartPose;
        if (hasPose) {
            float ax = prefs.getFloat("x", 0);
            float ay = prefs.getFloat("y", 0);
            float ah = prefs.getFloat("heading", 0);
            teleopStartPose = new Pose(ax, ay, ah);
        } else {
            teleopStartPose = startPose;
        }
        // Remove keys after reading so they're not reused again
        prefs.edit().remove("x").remove("y").remove("heading").apply();

        // Initialize the robot hardware
        robot.init();

        // Initialize FTC Dashboard
        dashboard = FtcDashboard.getInstance();
        packet = new TelemetryPacket();

        // Initialize the state machine with gamepad2 and the specified mode
        stateMachine = new RobotStateMachine(robot, telemetry, gamepad2, getRobotMode());
        stateMachine.setTeamColor(getTeamColor()); // Set team color once during initialization

        // Initialize IMU - Try Pinpoint first, fallback to REV IMU
        initializeIMU();

        // Initialize the follower with (possibly transferred) starting pose
        Constants.setConstants(FConstants.class, LConstants.class);
        follower = new Follower(hardwareMap, FConstants.class, LConstants.class);
        follower.setStartingPose(teleopStartPose);

        // Performance optimization: Cache hardware queries and enable bulk reading
        cachedHubs = hardwareMap.getAll(LynxModule.class);

        // Enable bulk reading for maximum performance
        for (LynxModule hub : cachedHubs) {
            hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
        }

        // Initialize hub colors to team color
        for (LynxModule hub : cachedHubs) {
            hub.setConstant(getTeamColor().hubColor);
        }

        telemetry.addData("Start Pose", String.format(Locale.US, "%.2f, %.2f, %.1f°", teleopStartPose.getX(), teleopStartPose.getY(), Math.toDegrees(teleopStartPose.getHeading())));
        telemetry.addData("Team Color", getTeamColor().displayName);
        telemetry.addData("Robot Mode", getRobotMode().toString());
        telemetry.addData("Bulk Reading", "ENABLED");
        telemetry.addData("Status", "Initialized");
        telemetry.update();
    }

    /** This method is called once at the start of the OpMode. **/
    @Override
    public void start() {
        follower.startTeleopDrive();
    }

    /** This is the main loop of the opmode and runs continuously after play **/
    @Override
    public void loop() {
        long loopStartNano = System.nanoTime(); // Start timing the loop
        long currentTimeMs = System.currentTimeMillis();
        double currentTimeSeconds = currentTimeMs / 1000.0; // Shared timestamp

        // Enhanced hub color management with team colors and sample feedback
        updateHubColors(currentTimeMs);

        // Check for sample detection feedback from state machine (improved performance)
        checkSampleDetectionFeedbackFromStateMachine(currentTimeMs);

        // Process D-pad for Orientation Lock using shared edge detection
        if (buttonDetectorDriver.dpadUpPressed(gamepad1)) {
            robot.driveSystem.lockOrientation(Orientation.FORWARD);
        } else if (buttonDetectorDriver.dpadLeftPressed(gamepad1)) {
            robot.driveSystem.lockOrientation(Orientation.LEFT);
        } else if (buttonDetectorDriver.dpadDownPressed(gamepad1)) {
            robot.driveSystem.lockOrientation(Orientation.BACK);
        } else if (buttonDetectorDriver.dpadRightPressed(gamepad1)) {
            robot.driveSystem.lockOrientation(Orientation.RIGHT);
        } else if (buttonDetectorDriver.xPressed(gamepad1)) {
            robot.driveSystem.lockOrientation(Orientation.BASKET);
        }

        double deltaTime = getDeltaTime();
        stateMachine.setButtonDetector(buttonDetectorIntakeOuttake);
        stateMachine.update();

        // Performance optimization: Only update telemetry periodically to reduce overhead
        boolean shouldUpdateTelemetry = (currentTimeMs - lastTelemetryUpdate) >= ControlConstants.TELEMETRY_UPDATE_INTERVAL_MS;
        if (shouldUpdateTelemetry) {
            telemetryBuilder.setLength(0);
            telemetryBuilder.append(stateMachine.getCurrentMode()).append(" | ").append(stateMachine.getCurrentState());
            telemetry.addData("State", telemetryBuilder.toString());

            telemetryBuilder.setLength(0);
            telemetryBuilder.append(stateMachine.getExternalState());
            telemetry.addData("ExtSpecimenState", telemetryBuilder.toString());

            if (ControlConstants.STATE_MACHINE_DEBUG_MODE) {
                telemetryBuilder.setLength(0);
                telemetryBuilder.append("ON");
                telemetry.addData("StateMachineDebug", telemetryBuilder.toString());
            }

            lastTelemetryUpdate = currentTimeMs;
        }

        // Yaw reset for field reorientation using shared edge detection
        if (buttonDetectorDriver.backPressed(gamepad1)) {
            resetIMUYaw();
            robot.driveSystem.setCustomHeadingDeg(0.0);
        }

        // Color recognition toggle (X button on gamepad1)
        if (buttonDetectorDriver.xPressed(gamepad1)) {
            colorRecognitionMode = !colorRecognitionMode;
            robot.visionSystem.setColorRecognitionEnabled(colorRecognitionMode);
        }

        // Update vision system for color recognition
        if (colorRecognitionMode) {
            robot.visionSystem.update();
        }

        double headingRad = getIMUHeadingRadians();

        // Handle orientation locking
        // Note: D-pad orientation lock and turn input for it are now handled inside DriveSystem
        robot.driveSystem.drive(gamepad1, headingRad, currentTimeSeconds);

        // Performance optimization: Only update telemetry periodically
        if (shouldUpdateTelemetry) {
            // Optimized orientation status display
            telemetryBuilder.setLength(0);
            if (robot.driveSystem.isOrientationLocked()) {
                telemetryBuilder.append("LOCKED - ").append(robot.driveSystem.getCurrentOrientation().toString());
                telemetryBuilder.append(" (").append(String.format(Locale.US, "%.1f°",
                    (robot.driveSystem.getCurrentOrientation() == Orientation.CUSTOM) ?
                        robot.driveSystem.getCustomHeadingDeg() :
                        Math.toDegrees(robot.driveSystem.getTargetHeadingRad()))).append(")");
            } else {
                telemetryBuilder.append("DISABLED");
            }
            telemetry.addData("Orientation", telemetryBuilder.toString());

            // Optimized heading info
            telemetryBuilder.setLength(0);
            telemetryBuilder.append("Hold: ").append(robot.driveSystem.activeHeadingHoldEnabled ? "ON" : "OFF");
            telemetryBuilder.append(" | Curr: ").append(String.format(Locale.US, "%.1f°", Math.toDegrees(headingRad)));
            telemetry.addData("Heading", telemetryBuilder.toString());
        }

        // --- Servo/slide/intake logic ---
        double slideSpeed = gamepad2.right_stick_y;
        double baseSpeed = gamepad2.left_trigger - gamepad2.right_trigger;
        double wristSpeed = -gamepad2.left_stick_x;
        // Performance optimization: Only update servo velocity if there's actual input
        if (!stateMachine.getIsIntaking()) {
            robot.setServoVelocity(robot.intake.getIntakeSlideLeft(), slideSpeed, deltaTime,
                    IntakeConstants.SLIDE_MAX, IntakeConstants.SLIDE_MIN,
                    ControlConstants.SLIDE_MAX_VELOCITY, shouldUpdateTelemetry ? telemetry : null);
            robot.setServoVelocity(robot.intake.getIntakeSlideRight(), slideSpeed, deltaTime,
                    IntakeConstants.SLIDE_MAX, IntakeConstants.SLIDE_MIN,
                    ControlConstants.SLIDE_MAX_VELOCITY, shouldUpdateTelemetry ? telemetry : null);

            robot.setServoVelocity(robot.intake.getIntakeWrist(), wristSpeed, deltaTime,
                    0, 1,
                    ControlConstants.WRIST_MAX_VELOCITY, shouldUpdateTelemetry ? telemetry : null);

            robot.setServoVelocity(robot.intake.getIntakeTurret(), baseSpeed, deltaTime,
                    IntakeConstants.TURRET_RIGHT, IntakeConstants.TURRET_LEFT,
                    ControlConstants.WRIST_MAX_VELOCITY, shouldUpdateTelemetry ? telemetry : null);
        }

        // Intake color sensor debug telemetry (optimized - only when needed)
        if (shouldUpdateTelemetry) {
            // Use the optimized method that doesn't call sensor every loop
            double[] rgbValues = robot.intake.getRawRGBValues();
            telemetry.addData("IntakeColor:R", String.format("%.0f", rgbValues[0]));
            telemetry.addData("IntakeColor:G", String.format("%.0f", rgbValues[1]));
            telemetry.addData("IntakeColor:B", String.format("%.0f", rgbValues[2]));
            telemetry.addData("IntakeColor:Dist(cm)", String.format("%.1f", rgbValues[3]));
            telemetry.addData("SampleColor Enum", stateMachine.getBatchedSampleColor());
            telemetry.addData("IntakeLocked", stateMachine.getIsIntaking() ? "Yes" : "No");
            telemetry.addData("OuttakeSlideLimit", robot.outtake.isSlideAtMinPosition() ? "PRESSED" : "OPEN");
            telemetry.addData("OuttakeResetStatus", stateMachine.isOuttakeResetInProgress() ? "RESETTING" : "READY");

            // Add the same data to FTC Dashboard for web viewing
            packet = new TelemetryPacket();
            packet.put("IntakeColor:R", String.format("%.0f", rgbValues[0]));
            packet.put("IntakeColor:G", String.format("%.0f", rgbValues[1]));
            packet.put("IntakeColor:B", String.format("%.0f", rgbValues[2]));
            packet.put("IntakeColor:Dist(cm)", String.format("%.1f", rgbValues[3]));
            packet.put("SampleColor Enum", stateMachine.getBatchedSampleColor().toString());
            packet.put("IntakeLocked", stateMachine.getIsIntaking() ? "Yes" : "No");
            packet.put("OuttakeSlideLimit", robot.outtake.isSlideAtMinPosition() ? "PRESSED" : "OPEN");
            packet.put("OuttakeResetStatus", stateMachine.isOuttakeResetInProgress() ? "RESETTING" : "READY");

            // Add state machine telemetry to dashboard
            packet.put("State", stateMachine.getCurrentMode() + " | " + stateMachine.getCurrentState());
            packet.put("ExtSpecimenState", stateMachine.getExternalState().toString());
            if (ControlConstants.STATE_MACHINE_DEBUG_MODE) {
                packet.put("StateMachineDebug", "ON");
            }

            // Add team color and status info
            packet.put("Team & Status", getTeamColor().displayName + " | " + stateMachine.getCurrentMode() +
                (stateMachine.isSampleFeedbackActive() ? " | " + stateMachine.getLastDetectedSample() + " DETECTED!" : ""));

            // Add loop performance data
            packet.put("Loop Performance", performanceMonitor.getPerformanceSummary());

            // Add color recognition data to dashboard
            packet.put("Color Recognition", colorRecognitionMode ? "ENABLED" : "DISABLED");
            if (colorRecognitionMode && robot.visionSystem.isColorSampleDetected()) {
                packet.put("Vision Sample", robot.visionSystem.getDetectedColor().toString());
                packet.put("Sample Angle", String.format("%.1f°", robot.visionSystem.getSampleAngle()));
                packet.put("Sample Quadrant", robot.visionSystem.isSampleInTopQuadrant() ? "TOP" :
                                            robot.visionSystem.isSampleInBottomQuadrant() ? "BOTTOM" : "NONE");
            }
        }

        // Update shared edge detector for next cycle
        buttonDetectorDriver.update(gamepad1);
        buttonDetectorIntakeOuttake.update(gamepad2);

        // Calculate loop time performance
        long loopEndNano = System.nanoTime();
        double currentLoopMs = (loopEndNano - loopStartNano) / 1_000_000.0; // Convert to milliseconds

        // Record loop time in performance monitor
        performanceMonitor.recordLoopTime(currentLoopMs);

        // Enhanced loop time calculation and telemetry
        if (shouldUpdateTelemetry) {
            // Comprehensive loop performance telemetry
            displayLoopPerformanceTelemetry();

            // Send dashboard packet for web viewing
            if (packet != null) {
                dashboard.sendTelemetryPacket(packet);
            }

            // Performance optimization: Only call telemetry.update() when we have new data
            telemetry.update();
        }
    }

    /** We do not use this because everything automatically should disable **/
    @Override
    public void stop() {
    }

    /**
     * Calculate the time elapsed since the last update
     *
     * @return Time in seconds since the last update
     */
    private double getDeltaTime() {
        long currentTime = System.currentTimeMillis();
        double deltaTime = (currentTime - lastUpdateTime) / 1000.0; // Convert to seconds
        lastUpdateTime = currentTime;
        return deltaTime;
    }

    /**
     * Enhanced hub color management with sophisticated blinking patterns
     */
    private void updateHubColors(long currentTimeMs) {
        long blinkInterval;
        boolean shouldUpdate = false;

        // Priority 1: Sample feedback with different blink speeds (using state machine data)
        if (stateMachine.isSampleFeedbackActive()) {
            // Different blink speeds for different sample types (using state machine data)
            SampleColor detectedSample = stateMachine.getLastDetectedSample();
            switch (detectedSample) {
                case RED:
                case BLUE:
                    // Alliance/opponent samples: fast blink if alliance, slow if opponent
                    boolean isAllianceSample = (detectedSample == SampleColor.RED && getTeamColor() == TeamColor.RED) ||
                                             (detectedSample == SampleColor.BLUE && getTeamColor() == TeamColor.BLUE);
                    blinkInterval = isAllianceSample ? ControlConstants.FAST_BLINK_INTERVAL_MS : ControlConstants.SLOW_BLINK_INTERVAL_MS;
                    break;
                case YELLOW:
                    // Neutral samples: ultra fast blink (most noticeable)
                    blinkInterval = ControlConstants.ULTRA_FAST_BLINK_INTERVAL_MS;
                    break;
                default:
                    blinkInterval = ControlConstants.FAST_BLINK_INTERVAL_MS;
                    break;
            }
            shouldUpdate = (currentTimeMs - lastColorToggleTime >= blinkInterval);
        }
        // Priority 2: Normal mode-based colors
        else {
            blinkInterval = 250; // Normal mode switching
            shouldUpdate = (currentTimeMs - lastColorToggleTime >= blinkInterval);
        }

        if (shouldUpdate) {
            lastColorToggleState = !lastColorToggleState;
            lastColorToggleTime = currentTimeMs;

            int targetColor;

            // Sample feedback colors (using state machine data)
            if (stateMachine.isSampleFeedbackActive()) {
                SampleColor detectedSample = stateMachine.getLastDetectedSample();
                if (lastColorToggleState) {
                    // Show sample color
                    switch (detectedSample) {
                        case RED:
                            targetColor = Color.RED;
                            break;
                        case BLUE:
                            targetColor = Color.BLUE;
                            break;
                        case YELLOW:
                            targetColor = Color.YELLOW;
                            break;
                        default:
                            targetColor = getTeamColor().hubColor;
                            break;
                    }
                } else {
                    // Off phase - show contrasting color for maximum visibility
                    switch (detectedSample) {
                        case RED:
                            targetColor = Color.BLACK;
                            break;
                        case BLUE:
                            targetColor = Color.BLACK;
                            break;
                        case YELLOW:
                            targetColor = Color.BLACK;
                            break;
                        default:
                            targetColor = Color.BLACK;
                            break;
                    }
                }
            }
            // Normal mode-based colors
            else {
                switch (stateMachine.getCurrentMode()) {
                    case SAMPLE:
                        // Gentle alternation between team color and dim for sample mode
                        targetColor = lastColorToggleState ? getTeamColor().hubColor : Color.rgb(64, 64, 0); // Dim yellow
                        break;
                    case SPECIMEN:
                        // Solid team color for specimen mode
                        targetColor = getTeamColor().hubColor;
                        break;
                    default:
                        targetColor = getTeamColor().hubColor;
                        break;
                }
            }

            // Apply color to all hubs
            for (LynxModule hub : cachedHubs) {
                hub.setConstant(targetColor);
            }
        }
    }

    /**
     * Check for sample detection feedback from state machine (improved performance)
     * Color detection is now handled in the state machine to reduce teleop loop time
     */
    private void checkSampleDetectionFeedbackFromStateMachine(long currentTimeMs) {
        // Get sample detection info from state machine instead of directly reading sensor
        if (stateMachine.isNewSampleDetected()) {
            lastDetectedSample = stateMachine.getLastDetectedSample();
            lastSampleFeedbackTime = currentTimeMs;
            isBlinkingForSample = true;

            // Start advanced rumble sequence
            isRumbleSequenceActive = true;
            rumbleSequenceStep = 0;
            lastRumbleTime = currentTimeMs;
        }

        // Update blinking state from state machine
        isBlinkingForSample = stateMachine.isSampleFeedbackActive();

        // Handle advanced rumble sequences
        if (isRumbleSequenceActive && gamepad1 != null) {
            handleRumbleSequence(currentTimeMs);
        }
    }

    /**
     * Handle sophisticated rumble sequences for different sample types
     */
    private void handleRumbleSequence(long currentTimeMs) {
        if (!isRumbleSequenceActive || gamepad1 == null) return;

        SampleColor detectedSample = stateMachine.getLastDetectedSample();
        boolean isAllianceSample = (detectedSample == SampleColor.RED && getTeamColor() == TeamColor.RED) ||
                                 (detectedSample == SampleColor.BLUE && getTeamColor() == TeamColor.BLUE);

        switch (detectedSample) {
            case RED:
            case BLUE:
                if (isAllianceSample) {
                    // Alliance sample: 3 quick pulses (short-short-short)
                    handleAllianceSampleRumble(currentTimeMs);
                } else {
                    // Opponent sample: 1 long pulse followed by 2 short
                    handleOpponentSampleRumble(currentTimeMs);
                }
                break;
            case YELLOW:
                // Neutral sample: Distinctive pattern - medium-short-medium
                handleNeutralSampleRumble(currentTimeMs);
                break;
            default:
                isRumbleSequenceActive = false;
                break;
        }
    }

    private void handleAllianceSampleRumble(long currentTimeMs) {
        long timeSinceStart = currentTimeMs - lastRumbleTime;

        switch (rumbleSequenceStep) {
            case 0: // First pulse
                if (timeSinceStart >= 0) {
                    gamepad1.rumble(0.9, 0.9, 120);
                    gamepad2.rumble(0.9, 0.9, 120);
                    rumbleSequenceStep++;
                    lastRumbleTime = currentTimeMs;
                }
                break;
            case 1: // Wait
                if (timeSinceStart >= 200) {
                    rumbleSequenceStep++;
                    lastRumbleTime = currentTimeMs;
                }
                break;
            case 2: // Second pulse
                if (timeSinceStart >= 0) {
                    gamepad1.rumble(0.9, 0.9, 120);
                    gamepad2.rumble(0.9, 0.9, 120);
                    rumbleSequenceStep++;
                    lastRumbleTime = currentTimeMs;
                }
                break;
            case 3: // Wait
                if (timeSinceStart >= 200) {
                    rumbleSequenceStep++;
                    lastRumbleTime = currentTimeMs;
                }
                break;
            case 4: // Third pulse
                if (timeSinceStart >= 0) {
                    gamepad1.rumble(0.9, 0.9, 120);
                    gamepad2.rumble(0.9, 0.9, 120);
                    rumbleSequenceStep++;
                    lastRumbleTime = currentTimeMs;
                }
                break;
            default:
                if (timeSinceStart >= 200) {
                    isRumbleSequenceActive = false;
                }
                break;
        }
    }

    private void handleOpponentSampleRumble(long currentTimeMs) {
        long timeSinceStart = currentTimeMs - lastRumbleTime;

        switch (rumbleSequenceStep) {
            case 0: // Long pulse
                if (timeSinceStart >= 0) {
                    gamepad1.rumble(1.0, 1.0, 400);
                    gamepad2.rumble(1.0, 1.0, 400);
                    rumbleSequenceStep++;
                    lastRumbleTime = currentTimeMs;
                }
                break;
            case 1: // Wait
                if (timeSinceStart >= 500) {
                    rumbleSequenceStep++;
                    lastRumbleTime = currentTimeMs;
                }
                break;
            case 2: // First short pulse
                if (timeSinceStart >= 0) {
                    gamepad1.rumble(0.7, 0.7, 100);
                    gamepad2.rumble(0.7, 0.7, 100);
                    rumbleSequenceStep++;
                    lastRumbleTime = currentTimeMs;
                }
                break;
            case 3: // Wait
                if (timeSinceStart >= 150) {
                    rumbleSequenceStep++;
                    lastRumbleTime = currentTimeMs;
                }
                break;
            case 4: // Second short pulse
                if (timeSinceStart >= 0) {
                    gamepad1.rumble(0.7, 0.7, 100);
                    gamepad2.rumble(0.7, 0.7, 100);
                    rumbleSequenceStep++;
                    lastRumbleTime = currentTimeMs;
                }
                break;
            default:
                if (timeSinceStart >= 150) {
                    isRumbleSequenceActive = false;
                }
                break;
        }
    }

    private void handleNeutralSampleRumble(long currentTimeMs) {
        long timeSinceStart = currentTimeMs - lastRumbleTime;

        switch (rumbleSequenceStep) {
            case 0: // Medium pulse
                if (timeSinceStart >= 0) {
                    gamepad1.rumble(0.8, 0.8, 200);
                    rumbleSequenceStep++;
                    lastRumbleTime = currentTimeMs;
                }
                break;
            case 1: // Wait
                if (timeSinceStart >= 250) {
                    rumbleSequenceStep++;
                    lastRumbleTime = currentTimeMs;
                }
                break;
            case 2: // Short pulse
                if (timeSinceStart >= 0) {
                    gamepad1.rumble(0.6, 0.6, 100);
                    rumbleSequenceStep++;
                    lastRumbleTime = currentTimeMs;
                }
                break;
            case 3: // Wait
                if (timeSinceStart >= 150) {
                    rumbleSequenceStep++;
                    lastRumbleTime = currentTimeMs;
                }
                break;
            case 4: // Final medium pulse
                if (timeSinceStart >= 0) {
                    gamepad1.rumble(0.8, 0.8, 200);
                    rumbleSequenceStep++;
                    lastRumbleTime = currentTimeMs;
                }
                break;
            default:
                if (timeSinceStart >= 250) {
                    isRumbleSequenceActive = false;
                }
                break;
        }
    }

    /**
     * Display comprehensive loop performance telemetry with all metrics
     */
    private void displayLoopPerformanceTelemetry() {
        // Team color and mode info with sample detection (using state machine data)
        telemetryBuilder.setLength(0);
        telemetryBuilder.append(getTeamColor().displayName).append(" | ").append(stateMachine.getCurrentMode());
        if (stateMachine.isSampleFeedbackActive()) {
            telemetryBuilder.append(" | 🔴 ").append(stateMachine.getLastDetectedSample()).append(" DETECTED!");
        }
        telemetry.addData("Team & Status", telemetryBuilder.toString());

        // Color recognition status and controls
        telemetryBuilder.setLength(0);
        telemetryBuilder.append("Vision: ").append(colorRecognitionMode ? "ON" : "OFF");
        telemetryBuilder.append(" | X=Toggle");
        telemetry.addData("Color Recognition", telemetryBuilder.toString());

        // Color recognition data (if enabled and sample detected)
        if (colorRecognitionMode) {
            if (robot.visionSystem.isColorSampleDetected()) {
                telemetryBuilder.setLength(0);
                telemetryBuilder.append(robot.visionSystem.getDetectedColor());
                telemetryBuilder.append(" | ").append(String.format("%.1f°", robot.visionSystem.getSampleAngle()));
                telemetryBuilder.append(" | ").append(robot.visionSystem.isSampleInTopQuadrant() ? "TOP" :
                                                   robot.visionSystem.isSampleInBottomQuadrant() ? "BOTTOM" : "NONE");
                telemetry.addData("Vision Sample", telemetryBuilder.toString());
            } else {
                telemetry.addData("Vision Sample", "No samples detected");
            }
        }

        // Essential loop metrics using performance monitor
        telemetry.addData("Loop Performance", performanceMonitor.getPerformanceSummary());
    }

    /**
     * Initialize IMU - Try Pinpoint first, fallback to REV IMU
     */
    private void initializeIMU() {
        try {
            // Try to initialize Pinpoint IMU first
            // Using reflection to avoid compile-time dependency on Pinpoint driver
            Class<?> pinpointClass = Class.forName("com.qualcomm.hardware.gobilda.GoBildaPinpointDriver");
            imu = hardwareMap.get(pinpointClass, "pinpoint");

            // Initialize Pinpoint IMU
            // pinpoint.setOffsets(-84.0, -168.0); // Example offsets - adjust for your robot
            // pinpoint.setEncoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD);
            // pinpoint.setEncoderDirections(GoBildaPinpointDriver.EncoderDirection.FORWARD, GoBildaPinpointDriver.EncoderDirection.FORWARD);
            // pinpoint.resetPosAndIMU();

            // Use reflection to call methods
            java.lang.reflect.Method resetMethod = pinpointClass.getMethod("resetPosAndIMU");
            resetMethod.invoke(imu);

            usingPinpointIMU = true;
            telemetry.addData("IMU", "Pinpoint IMU initialized successfully");

        } catch (Exception e) {
            // Fallback to REV IMU
            try {
                BHI260IMU revIMU = hardwareMap.get(BHI260IMU.class, "imu");
                BHI260IMU.Parameters imuParameters = new BHI260IMU.Parameters(
                        new RevHubOrientationOnRobot(
                                RevHubOrientationOnRobot.LogoFacingDirection.LEFT,
                                RevHubOrientationOnRobot.UsbFacingDirection.FORWARD
                        )
                );
                revIMU.initialize(imuParameters);
                revIMU.resetYaw();

                imu = revIMU;
                usingPinpointIMU = false;
                telemetry.addData("IMU", "REV IMU initialized (Pinpoint not available)");

            } catch (Exception revError) {
                telemetry.addData("ERROR", "Failed to initialize any IMU: " + revError.getMessage());
                imu = null;
                usingPinpointIMU = false;
            }
        }
        telemetry.update();
    }

    /**
     * Reset IMU yaw for both Pinpoint and REV IMU
     */
    private void resetIMUYaw() {
        if (imu == null) return;

        try {
            if (usingPinpointIMU) {
                // Reset Pinpoint IMU using reflection
                Class<?> pinpointClass = imu.getClass();
                java.lang.reflect.Method resetMethod = pinpointClass.getMethod("resetPosAndIMU");
                resetMethod.invoke(imu);
            } else {
                // Reset REV IMU
                ((BHI260IMU) imu).resetYaw();
            }
        } catch (Exception e) {
            telemetry.addData("ERROR", "Failed to reset IMU yaw: " + e.getMessage());
        }
    }

    /**
     * Get heading in radians from either Pinpoint or REV IMU
     */
    private double getIMUHeadingRadians() {
        if (imu == null) return 0.0;

        try {
            if (usingPinpointIMU) {
                // Get heading from Pinpoint IMU using reflection
                Class<?> pinpointClass = imu.getClass();
                java.lang.reflect.Method updateMethod = pinpointClass.getMethod("update");
                updateMethod.invoke(imu);

                java.lang.reflect.Method getHeadingMethod = pinpointClass.getMethod("getHeading", AngleUnit.class);
                return (Double) getHeadingMethod.invoke(imu, AngleUnit.RADIANS);
            } else {
                // Get heading from REV IMU
                return ((BHI260IMU) imu).getRobotOrientation(
                        AxesReference.INTRINSIC,
                        AxesOrder.ZYX,
                        AngleUnit.RADIANS).firstAngle;
            }
        } catch (Exception e) {
            telemetry.addData("ERROR", "Failed to get IMU heading: " + e.getMessage());
            return 0.0;
        }
    }

    /**
     * Get the state machine instance for external access
     * @return The robot state machine
     */
    public RobotStateMachine getStateMachine() {
        return stateMachine;
    }

    /**
     * Get the robot hardware instance for external access
     * @return The robot hardware
     */
    protected RobotHardware getRobot() {
        return robot;
    }
}
