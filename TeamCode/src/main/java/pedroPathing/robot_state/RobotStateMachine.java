package pedroPathing.robot_state;

import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.RobotLog;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;

import pedroPathing.constants.ControlConstants;
import pedroPathing.constants.IntakeConstants;
import pedroPathing.constants.OuttakeConstants;
import pedroPathing.hardware.RobotHardware;
import pedroPathing.hardware.SampleColor;
import pedroPathing.util.ButtonEdgeDetector;

public class RobotStateMachine {
    // Core state machine components
    private RobotMode currentMode = RobotMode.SPECIMEN;
    private RobotState currentState = RobotState.INIT;
    private RobotState externalState = RobotState.INIT;
    private final ElapsedTime stateTimer = new ElapsedTime();
    private final ElapsedTime externalStateTimer = new ElapsedTime();
    private final RobotHardware robot;
    private final Telemetry telemetry;
    private Gamepad gamepad;
    private ButtonEdgeDetector buttonDetector;

    // Debug mode for testing and development (controlled by ControlConstants.STATE_MACHINE_DEBUG_MODE)

    // Team color (set from teleop)
    private String teamColorName = null;

    // Static state variables
    public static boolean isIntaking = false;
    public static SampleColor intakeSample = SampleColor.NONE;
    public static SampleColor outtakeSample = SampleColor.NONE;

    // State-specific color checking (only during intake check states for 1 second)
    private boolean isInColorCheckState = false;
    private long colorCheckStateStartTime = 0;
    private SampleColor colorCheckResult = SampleColor.NONE;

    // Sample feedback system
    private SampleColor lastDetectedSample = SampleColor.NONE;
    private long lastSampleFeedbackTime = 0;
    private boolean isBlinkingForSample = false;
    private boolean newSampleDetected = false;

    // Outtake reset system
    private final ElapsedTime outtakeResetTimer = new ElapsedTime();
    private boolean scheduleOuttakeReset = false;
    private long outtakeResetDelay = ControlConstants.OUTTAKE_RESET_DELAY_MS;
    private volatile boolean outtakeResetDone = true;

    // Intake slide reset system (for independent slide movement timing)
    private final ElapsedTime intakeSlideResetTimer = new ElapsedTime();
    private boolean scheduleIntakeSlideReset = false;
    private long intakeSlideResetDelay = IntakeConstants.SLIDE_RELEASE_TIME;
    private volatile boolean intakeSlideResetDone = true;

    // Performance optimizations: Pre-computed arrays for O(1) state transitions
    private StateTransition[][] sampleTransitionArray;
    private StateTransition[][] specimenTransitionArray;
    private StateTransition[][] specimenExternalTransitionArray;

    // Track executed actions for run-once behavior
    private final Set<StateTransition> executedOnceActions = new HashSet<>();

    // End delay tracking
    private final Map<StateTransition, ElapsedTime> endDelayTimers = new HashMap<>();
    private final Set<StateTransition> waitingForEndDelay = new HashSet<>();

    // Lightweight StateTransition class
    private static class StateTransition {
        final RobotState currentState;
        final double waitTimeMs;
        final BooleanSupplier condition;
        final Runnable action;
        final RobotState nextState;
        final double endDelayMs;

        StateTransition(RobotState currentState, double waitTimeMs, BooleanSupplier condition, Runnable action, RobotState nextState, double endDelayMs) {
            this.currentState = currentState;
            this.waitTimeMs = waitTimeMs;
            this.condition = condition;
            this.action = action;
            this.nextState = nextState;
            this.endDelayMs = endDelayMs;
        }

        // Backward compatibility constructor
        StateTransition(RobotState currentState, double waitTimeMs, BooleanSupplier condition, Runnable action, RobotState nextState) {
            this(currentState, waitTimeMs, condition, action, nextState, 0);
        }
    }

    // Transition maps (converted to arrays for performance)
    private Map<RobotState, List<StateTransition>> sampleTransitions;
    private Map<RobotState, List<StateTransition>> specimenTransitions;
    private Map<RobotState, List<StateTransition>> specimenExternalTransitions;

    public RobotStateMachine(RobotHardware robot, Telemetry telemetry, Gamepad gamepad, RobotMode initialMode) {
        this.robot = robot;
        this.telemetry = telemetry;
        this.gamepad = gamepad;
        this.currentMode = initialMode; // Set the initial mode

        // Pre-allocate transition arrays for O(1) access
        int stateCount = RobotState.values().length;
        sampleTransitionArray = new StateTransition[stateCount][];
        specimenTransitionArray = new StateTransition[stateCount][];
        specimenExternalTransitionArray = new StateTransition[stateCount][];

        // Initialize state machine based on initial mode
        if (currentMode == RobotMode.SPECIMEN) {
            changeState(RobotState.HOLD);
        } else {
            changeState(RobotState.INIT);
        }

        initSampleTransitions();
        initSpecimenTransitions();
        initSpecimenExternalTransitions();
        convertTransitionsToArrays();

        RobotLog.dd("RobotStateMachine", "Initialized with mode: %s", initialMode);
    }

    // Backward compatibility constructor (defaults to SPECIMEN mode)
    public RobotStateMachine(RobotHardware robot, Telemetry telemetry, Gamepad gamepad) {
        this(robot, telemetry, gamepad, RobotMode.SPECIMEN);
    }

    // Initialize the Sample state transition table
    private void initSampleTransitions() {
        sampleTransitions = new HashMap<>();
        sampleTransitions.put(
                RobotState.INIT,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.INIT,
                                0,
                                null,
                                () -> {
                                    robot.outtake.setElbowPosition(OuttakeConstants.ELBOW_REST);
                                    robot.outtake.setWristPosition(OuttakeConstants.ELBOW_REST);
                                    robot.intake.setIntakeClawPosition(IntakeConstants.CLAW_OPEN);
                                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_REST);
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_REST);
                                    robot.intake.setIntakeWristPosition(IntakeConstants.WRIST_MIDDLE);
                                },
                                RobotState.SAMPLE_INTAKE_TARGET_CHECK
                        )
                )
        );
        sampleTransitions.put(
                RobotState.SAMPLE_INTAKE_TARGET_CHECK,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_TARGET_CHECK,
                                0,
                                () -> buttonDetector.rightBumperPressed(gamepad),
                                null,
                                RobotState.SAMPLE_INTAKE_TARGET
                        )
                )
        );

        sampleTransitions.put(
                RobotState.SAMPLE_INTAKE_TARGET,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_TARGET,
                                0,
                                null,
                                () -> {
                                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_PREP);
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_PREP);
                                },
                                RobotState.SAMPLE_INTAKE_POSITION_CHECK
                        )
                )
        );

        sampleTransitions.put(
                RobotState.SAMPLE_INTAKE_POSITION_CHECK,
                Arrays.asList(
                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_POSITION_CHECK,
                                0,
                                () -> buttonDetector.rightBumperPressed(gamepad),
                                null,
                                RobotState.SAMPLE_INTAKE_GRAB
                        ),
                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_POSITION_CHECK,
                                0,
                                () -> buttonDetector.yPressed(gamepad),
                                null,
                                RobotState.INIT
                        )
                )
        );
        sampleTransitions.put(
                RobotState.SAMPLE_INTAKE_GRAB_RESET,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_GRAB_RESET,
                                IntakeConstants.CLAW_CLOSED_TIME,
                                null,
                                null,
                                RobotState.SAMPLE_INTAKE_GRAB
                        )
                )
        );

        sampleTransitions.put(
                RobotState.SAMPLE_INTAKE_GRAB,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_GRAB,
                                0,
                                null,
                                () -> {
                                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_GRAB);
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_GRAB);
                                    },
                                RobotState.SAMPLE_INTAKE_CLOSE
                        )
                )
        );



        sampleTransitions.put(
                RobotState.SAMPLE_INTAKE_CLOSE,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_CLOSE,
                                IntakeConstants.INTAKE_SHOULDER_GRAB_CLOSE_TIME,
                                null,
                                () -> robot.intake.closeIntakeClaw(),
                                RobotState.SAMPLE_INTAKE_CLAW_CLOSE
                        )
                )
        );

        sampleTransitions.put(
                RobotState.SAMPLE_INTAKE_CLAW_CLOSE,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_CLAW_CLOSE,
                                IntakeConstants.CLAW_CLOSED_TIME,
                                null,
                                () -> {
                                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_PREP);
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_PREP);
                                },
                                RobotState.SAMPLE_INTAKE_CHECK_SAMPLE
                        )


                )
        );

        sampleTransitions.put(
                RobotState.SAMPLE_INTAKE_CHECK_SAMPLE,
                Arrays.asList(
                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_CHECK_SAMPLE,
                                0,
                                () -> {
                                    // Formal color checking with distance validation
                                    ColorCheckResult colorResult = getFormalColorCheckResult();
                                    return buttonDetector.dpadDownPressed(gamepad) ||
                                            (ControlConstants.ENABLE_AUTO_COLOR_ADVANCE &&
                                             colorResult.hasValidSample() &&
                                             isAllianceOrNeutralSample(colorResult.color));
                                },
                         null,
                                RobotState.SAMPLE_INTAKE_WRIST_MOVE
                        ),


                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_CHECK_SAMPLE,
                                0,
                                () -> {
                                    // Formal color checking with distance validation
                                    ColorCheckResult colorResult = getFormalColorCheckResult();
                                    return buttonDetector.dpadRightPressed(gamepad) ||
                                            (ControlConstants.ENABLE_AUTO_REMOVE_OPPONENT_SAMPLE &&
                                             colorResult.hasValidSample() &&
                                             isOpponentSample(colorResult.color));
                                },
                                null,
                                RobotState.INTAKE_REMOVE_OPPONENT_SAMPLE
                        ),

                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_CHECK_SAMPLE,
                                0,
                                () -> buttonDetector.rightBumperPressed(gamepad),
                                () -> robot.intake.setIntakeClawPosition(IntakeConstants.CLAW_OPEN),
                                RobotState.SAMPLE_INTAKE_GRAB_RESET
                        ),


                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_CLAW_CLOSE,
                                0,
                                () -> buttonDetector.yPressed(gamepad),
                                null,
                                RobotState.INIT
                        )
                )
        );

        sampleTransitions.put(
                RobotState.SAMPLE_INTAKE_WRIST_MOVE,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_WRIST_MOVE,
                                0,
                                null,
                                () -> {
                                    isIntaking = true;
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_GRAB);
                                    robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_HOLD);
                                    robot.intake.setIntakeWristPosition(IntakeConstants.WRIST_MIDDLE);
                                    robot.intake.setIntakeTurretPosition(IntakeConstants.TURRET_MIDDLE);

                                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_OUTTAKE_TRANSITION);

                                    robot.outtake.setElbowPosition(OuttakeConstants.ELBOW_TRANSITION);
                                    robot.outtake.setWristPosition(OuttakeConstants.WRIST_TRANSITION);
                                },
                                RobotState.SAMPLE_INTAKE_ELBOWS_PREP
                        )
                )
        );

        sampleTransitions.put(
                RobotState.SAMPLE_INTAKE_ELBOWS_PREP,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_ELBOWS_PREP,
                                IntakeConstants.INTAKE_ELBOWS_PREP,
                                null,
                                () -> {
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_OUTTAKE_TRANSITION_PREP);
                                },
                                RobotState.SAMPLE_INTAKE_RELEASE_ELBOWS
                        )
                )
        );

        sampleTransitions.put(
                RobotState.SAMPLE_INTAKE_RELEASE_ELBOWS,
                Arrays.asList(
                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_RELEASE_ELBOWS,
                                IntakeConstants.INTAKE_LIMIT_SWITCH_DELAY,
                                () -> robot.intake.isSlideAtHoldPosition(),
                                () -> {
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_OUTTAKE_TRANSITION);
                                },
                                RobotState.SAMPLE_INTAKE_SLIDES_CONTRACT,
                                IntakeConstants.SLIDE_HOLD_POSITION_END_DELAY
                        ),
                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_RELEASE_ELBOWS,
                                IntakeConstants.INTAKE_SHOULDER_OUTTAKE_TRANSITION_PREP_TIME,
                                null,
                                () -> {
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_OUTTAKE_TRANSITION);
                                },
                                RobotState.SAMPLE_INTAKE_SLIDES_CONTRACT
                        )
                )
        );



        sampleTransitions.put(
                RobotState.SAMPLE_INTAKE_SLIDES_CONTRACT,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_SLIDES_CONTRACT,
                                OuttakeConstants.CLAW_CLOSED_TIME,
                                null,
                                () -> {
                                    robot.outtake.setClawPosition(OuttakeConstants.CLAW_CLOSED);
                                    robot.intake.setIntakeClawPosition(IntakeConstants.CLAW_OPEN);
                                },
                                RobotState.SAMPLE_INTAKE_CLAW_OPEN
                        )
                )
        );
        sampleTransitions.put(
                RobotState.SAMPLE_INTAKE_CLAW_OPEN,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SAMPLE_INTAKE_CLAW_OPEN,
                                OuttakeConstants.CLAW_CLOSED_TIME,
                                null,
                                () -> {
                                    isIntaking = false;
                                    robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_RELEASE);
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_REST);
                                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_REST);

                                    robot.outtake.setElbowPosition(OuttakeConstants.ELBOW_REST);
                                    robot.outtake.setElbowPosition(OuttakeConstants.ELBOW_REST);
                                    robot.outtake.setWristPosition(OuttakeConstants.WRIST_REST);

                                    // Schedule the slide to move to SLIDE_MIN after SLIDE_RELEASE_TIME
                                    scheduleIntakeSlideReset(IntakeConstants.SLIDE_RELEASE_TIME);
                                },
                                RobotState.COMPLETE_INTAKE
                        )
                )
        );


        // SAMPLE_INTAKE_RESET_SLIDE state removed - slide movement now handled by scheduled reset system
        // COMPLETE_INTAKE: left_bumper for basket, right_bumper for claw
        sampleTransitions.put(
                RobotState.COMPLETE_INTAKE,
                Arrays.asList(
                        new StateTransition(
                                RobotState.COMPLETE_INTAKE,
                                0,
                                () -> buttonDetector.leftBumperPressed(gamepad),
                                () -> {
                                    robot.outtake.setSlidePosition(OuttakeConstants.SLIDE_BASKET);
                                    robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_HOLD);
                                },
                                RobotState.SAMPLE_OUTTAKE_SLIDES_EXTEND
                        ),
                        new StateTransition(
                                RobotState.COMPLETE_INTAKE,
                                0,
                                () -> buttonDetector.rightBumperPressed(gamepad),
                                () -> {
                                    robot.outtake.setClawPosition(OuttakeConstants.CLAW_OPEN);
                                    robot.intake.setIntakeClawPosition(IntakeConstants.CLAW_OPEN);
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_PREP);
                                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_PREP);

                                },
                                RobotState.SAMPLE_INTAKE_POSITION_CHECK
                        )
                )
        );
        sampleTransitions.put(
                RobotState.SAMPLE_OUTTAKE_SLIDES_EXTEND,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SAMPLE_OUTTAKE_SLIDES_EXTEND,
                                OuttakeConstants.SLIDE_ELBOW_PREP,
                                null,
                                () -> {
                                    robot.outtake.setElbowPosition(OuttakeConstants.ELBOW_BASKET);
                                    robot.outtake.setWristPosition(OuttakeConstants.WRIST_BASKET);
                                },
                                RobotState.SAMPLE_OUTTAKE_ELBOW_BASKET
                        )
                )
        );
        sampleTransitions.put(
                RobotState.SAMPLE_OUTTAKE_ELBOW_BASKET,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SAMPLE_OUTTAKE_ELBOW_BASKET,
                                500,
                                () -> buttonDetector.leftBumperPressed(gamepad),
                                () -> {
                                    robot.outtake.setClawPosition(OuttakeConstants.CLAW_OPEN);
                                },
                                RobotState.SAMPLE_OUTTAKE_DUMP
                        )
                )
        );
        sampleTransitions.put(
                RobotState.SAMPLE_OUTTAKE_DUMP,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SAMPLE_OUTTAKE_DUMP,
                                200,
                                null,
                                () -> {
                                    robot.outtake.setSlidePosition(OuttakeConstants.SLIDE_MIN);
                                    robot.outtake.setElbowPosition(OuttakeConstants.ELBOW_REST);
                                    robot.outtake.setWristPosition(OuttakeConstants.ELBOW_REST);
                                    outtakeResetDone = false;
                                    scheduleOuttakeReset(ControlConstants.OUTTAKE_RESET_DELAY_LONG_MS);
                                },
                                RobotState.INIT
                        )
                )
        );

        sampleTransitions.put(
                RobotState.INTAKE_REMOVE_OPPONENT_SAMPLE,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.INTAKE_REMOVE_OPPONENT_SAMPLE,
                                0,
                                null,
                                () -> {
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_REMOVE);
                                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_REMOVE);
                                    robot.intake.setIntakeTurretPosition(IntakeConstants.TURRET_REMOVE);
                                },
                                RobotState.INTAKE_RELEASE_OPPONENT_SAMPLE
                        )
                )
        );

        sampleTransitions.put(
                RobotState.INTAKE_RELEASE_OPPONENT_SAMPLE,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.INTAKE_RELEASE_OPPONENT_SAMPLE,
                                IntakeConstants.SAMPLE_REMOVE_TIME,
                                null,
                                () -> {
                                    robot.intake.setIntakeClawPosition(IntakeConstants.CLAW_OPEN);
                                },
                                RobotState.INTAKE_REMOVE_RESET_TURRET
                        )
                )
        );

        sampleTransitions.put(
                RobotState.INTAKE_REMOVE_RESET_TURRET,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.INTAKE_REMOVE_RESET_TURRET,
                                IntakeConstants.CLAW_CLOSED_TIME,
                                null,
                                () -> {
                                    robot.intake.setIntakeTurretPosition(IntakeConstants.TURRET_MIDDLE);
                                },
                                RobotState.INTAKE_REMOVE_RESET
                        )
                )
        );

        sampleTransitions.put(
                RobotState.INTAKE_REMOVE_RESET,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.INTAKE_REMOVE_RESET,
                                200,
                                null,
                                null,
                                RobotState.SAMPLE_INTAKE_TARGET
                        )
                )
        );
    }

    // Initialize the Specimen state transition table
    private void initSpecimenTransitions() {
        specimenTransitions = new HashMap<>();
        specimenTransitions.put(
                RobotState.INIT,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.INIT,
                                0,
                                null,
                                () -> {
                                    robot.outtake.openClaw();
                                    robot.outtake.setElbowPosition(OuttakeConstants.ELBOW_GET_SPECIMEN);
                                    robot.outtake.setWristPosition(OuttakeConstants.WRIST_GET_SPECIMEN);
                                },
                                RobotState.SPECIMEN_OUTTAKE_CLAW_CLOSE
                        )
                )
        );
        specimenTransitions.put(
                RobotState.SPECIMEN_OUTTAKE_CLAW_CLOSE,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SPECIMEN_OUTTAKE_CLAW_CLOSE,
                                IntakeConstants.CLAW_CLOSED_TIME,
                                () -> {
                                    // Formal specimen detection with distance validation
                                    SpecimenDetectionResult specimenResult = getFormalSpecimenDetectionResult();
                                    return (buttonDetector.leftBumperPressed(gamepad) ||
                                           (ControlConstants.ENABLE_AUTO_SPECIMEN_OUTTAKE_ADVANCE && specimenResult.hasValidSpecimen())) && outtakeResetDone;
                                },
                                () -> {
                                    robot.outtake.setClawPosition(OuttakeConstants.CLAW_CLOSED);
                                },
                                RobotState.SPECIMEN_OUTTAKE_PREPARE
                        )
                )
        );

        specimenTransitions.put(
                RobotState.SPECIMEN_OUTTAKE_PREPARE,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SPECIMEN_OUTTAKE_PREPARE,
                                OuttakeConstants.CLAW_FULL_CLOSED_TIME,
                                null,
                                () -> {
                                    robot.outtake.setSlidePosition(OuttakeConstants.SLIDE_LIFT);
                                    robot.outtake.setElbowPosition(OuttakeConstants.ELBOW_SCORE_SPECIMEN);
                                    robot.outtake.setWristPosition(OuttakeConstants.WRIST_SCORE_SPECIMEN);
                                    },
                                RobotState.SPECIMEN_OUTTAKE_CHECK
                        )
                )
        );
        specimenTransitions.put(
                RobotState.SPECIMEN_OUTTAKE_CHECK,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SPECIMEN_OUTTAKE_CHECK,
                                400,
                                () -> buttonDetector.leftBumperPressed(gamepad),
                                () -> robot.outtake.setSlidePosition(OuttakeConstants.SLIDE_SCORE),
                                RobotState.SPECIMEN_OUTTAKE_RELEASE
                        )
                )
        );
        specimenTransitions.put(
                RobotState.SPECIMEN_OUTTAKE_RELEASE,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SPECIMEN_OUTTAKE_RELEASE,
                                ControlConstants.SPECIMEN_RELEASE_DELAY_MS,
                                null,
                                () -> robot.outtake.setClawPosition(OuttakeConstants.CLAW_FULLY_OPEN),
                                RobotState.SPECIMEN_OUTTAKE_SCORE
                        )
                )
        );
        specimenTransitions.put(
                RobotState.SPECIMEN_OUTTAKE_SCORE,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SPECIMEN_OUTTAKE_SCORE,
                                OuttakeConstants.CLAW_CLOSED_TIME,
                                null,
                                () -> {
                                    robot.outtake.setSlidePosition(OuttakeConstants.SLIDE_MIN);
                                    outtakeResetDone = false;
                                    scheduleOuttakeReset(ControlConstants.OUTTAKE_RESET_DELAY_MS);
                                },
                                RobotState.INIT
                        )
                )
        );
        specimenTransitions.put(
                RobotState.HOLD,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.HOLD,
                                0,
                                null,
                                () -> {
                                    robot.outtake.setElbowPosition(OuttakeConstants.ELBOW_REST);
                                    robot.outtake.setWristPosition(OuttakeConstants.WRIST_TUCK);
                                    robot.outtake.setClawPosition(OuttakeConstants.CLAW_FULLY_OPEN);
                                    scheduleOuttakeReset(0);
                                },
                                RobotState.HOLD
                        )
                )
        );
    }

    // Initialize the Specimen EXTERNAL state transition table
    private void initSpecimenExternalTransitions() {
        specimenExternalTransitions = new HashMap<>();

        // Step 1: Wait for driver input, then move elbow to grab
        specimenExternalTransitions.put(
                RobotState.INIT,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.INIT,
                                0,
                                null,
                                () -> {
                                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_REST);
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_REST);
                                    robot.intake.setIntakeWristPosition(IntakeConstants.WRIST_MIDDLE);
                                    robot.intake.setIntakeClawPosition(IntakeConstants.CLAW_OPEN);
                                },
                                RobotState.SPECIMEN_INTAKE_TARGET_CHECK
                        )
                )
        );
        specimenExternalTransitions.put(
                RobotState.SPECIMEN_INTAKE_TARGET_CHECK,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SPECIMEN_INTAKE_TARGET_CHECK,
                                0,
                                () -> buttonDetector.rightBumperPressed(gamepad),
                                null,
                                RobotState.SPECIMEN_INTAKE_TARGET
                        )
                )
        );

        // Below state waits for right bumper, then moves elbow for grab
        specimenExternalTransitions.put(
                RobotState.SPECIMEN_INTAKE_TARGET,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SPECIMEN_INTAKE_TARGET,
                                0,
                                null,
                                () -> {
                                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_PREP);
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_PREP);
                                },
                                RobotState.SPECIMEN_INTAKE_POSITION_CHECK
                        )
                )
        );

        // Below state waits for right bumper, then moves elbow for grab
        specimenExternalTransitions.put(
                RobotState.SPECIMEN_INTAKE_POSITION_CHECK,
                Arrays.asList(
                        new StateTransition(
                                RobotState.SPECIMEN_INTAKE_POSITION_CHECK,
                                0,
                                () -> buttonDetector.rightBumperPressed(gamepad),
                                null,
                                RobotState.SPECIMEN_INTAKE_GRAB
                        ),
                        new StateTransition(
                                RobotState.SPECIMEN_INTAKE_POSITION_CHECK,
                                0,
                                () -> buttonDetector.yPressed(gamepad),
                                null,
                                RobotState.INIT
                        )
                )
        );

        specimenExternalTransitions.put(
                RobotState.SPECIMEN_INTAKE_GRAB_RESET,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SPECIMEN_INTAKE_GRAB_RESET,
                                IntakeConstants.CLAW_CLOSED_TIME,
                                null,
                                null,
                                RobotState.SPECIMEN_INTAKE_GRAB
                        )
                )
        );

        specimenExternalTransitions.put(
                RobotState.SPECIMEN_INTAKE_GRAB,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SPECIMEN_INTAKE_GRAB,
                                0,
                                null,
                                () -> {
                                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_GRAB);
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_GRAB);
                                },
                                RobotState.SPECIMEN_INTAKE_CLOSE
                        )
                )
        );

        specimenExternalTransitions.put(
                RobotState.SPECIMEN_INTAKE_CLOSE,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SPECIMEN_INTAKE_CLOSE,
                                IntakeConstants.INTAKE_SHOULDER_GRAB_CLOSE_TIME,
                                null,
                                () -> robot.intake.setIntakeClawPosition(IntakeConstants.CLAW_CLOSED),
                                RobotState.SPECIMEN_INTAKE_RESET_PICKED
                        )
                )
        );

        specimenExternalTransitions.put(
                RobotState.SPECIMEN_INTAKE_RESET_PICKED,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SPECIMEN_INTAKE_RESET_PICKED,
                                IntakeConstants.CLAW_CLOSED_TIME,
                                null,
                                () -> {
                                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_PREP);
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_PREP);
                                },
                                RobotState.SPECIMEN_INTAKE_CHECK_SAMPLE
                        )
                )
        );

        specimenExternalTransitions.put(
                RobotState.SPECIMEN_INTAKE_CHECK_SAMPLE,
                Arrays.asList(
                        new StateTransition(
                                RobotState.SPECIMEN_INTAKE_CHECK_SAMPLE,
                                0,
                                () -> {
                                    // Formal color checking with distance validation
                                    ColorCheckResult colorResult = getFormalColorCheckResult();
                                    return buttonDetector.dpadDownPressed(gamepad) ||
                                            (ControlConstants.ENABLE_AUTO_COLOR_ADVANCE &&
                                             colorResult.hasValidSample() &&
                                             isAllianceSample(colorResult.color));
                                },
                                null,
                                RobotState.SPECIMEN_INTAKE_CLAW_CLOSE
                        ),

                        new StateTransition(
                                RobotState.SPECIMEN_INTAKE_CHECK_SAMPLE,
                                0,
                                () -> {
                                    // Formal color checking with distance validation
                                    ColorCheckResult colorResult = getFormalColorCheckResult();
                                    return buttonDetector.dpadRightPressed(gamepad) ||
                                            (ControlConstants.ENABLE_AUTO_REMOVE_OPPONENT_SAMPLE &&
                                             colorResult.hasValidSample() &&
                                             isOpponentSample(colorResult.color));
                                },
                                null,
                                RobotState.INTAKE_REMOVE_OPPONENT_SAMPLE
                        ),


                        new StateTransition(
                                RobotState.SPECIMEN_INTAKE_CHECK_SAMPLE,
                                0,
                                () -> buttonDetector.rightBumperPressed(gamepad),
                                () -> robot.intake.setIntakeClawPosition(IntakeConstants.CLAW_OPEN),
                                RobotState.SPECIMEN_INTAKE_GRAB_RESET
                        )
                )
        );



        specimenExternalTransitions.put(
                RobotState.SPECIMEN_INTAKE_CLAW_CLOSE,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SPECIMEN_INTAKE_CLAW_CLOSE,
                                0,
                                null,
                                () -> {
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_HOLD);
                                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_HOLD);
                                    robot.intake.setIntakeTurretPosition(IntakeConstants.TURRET_MIDDLE);
                                    robot.intake.setIntakeWristPosition(IntakeConstants.WRIST_MIDDLE);
                                },
                                RobotState.SPECIMEN_INTAKE_READY
                        )
                )
        );

        specimenExternalTransitions.put(
                RobotState.SPECIMEN_INTAKE_READY,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SPECIMEN_INTAKE_READY,
                                0,
                                () -> buttonDetector.rightBumperPressed(gamepad),
                                null,
                                RobotState.SPECIMEN_INTAKE_RETRIEVE_SAMPLE
                        )
                )
        );
        specimenExternalTransitions.put(
                RobotState.SPECIMEN_INTAKE_RETRIEVE_SAMPLE,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SPECIMEN_INTAKE_RETRIEVE_SAMPLE,
                                0,
                                () -> buttonDetector.rightBumperPressed(gamepad),
                                () -> {
                                    robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MAX_OVERRIDE);
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_RELEASE);
                                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_RELEASE);
                                },
                                RobotState.SPECIMEN_INTAKE_EXTEND_SLIDE
                        )
                )
        );
        specimenExternalTransitions.put(
                RobotState.SPECIMEN_INTAKE_EXTEND_SLIDE,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SPECIMEN_INTAKE_EXTEND_SLIDE,
                                IntakeConstants.INTAKE_CLAW_RELEASE,
                                null,
                                () -> robot.intake.setIntakeClawPosition(IntakeConstants.CLAW_OPEN),
                                RobotState.SPECIMEN_INTAKE_RELEASE_SAMPLE
                        )
                )
        );
        specimenExternalTransitions.put(
                RobotState.SPECIMEN_INTAKE_RELEASE_SAMPLE,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.SPECIMEN_INTAKE_RELEASE_SAMPLE,
                                IntakeConstants.CLAW_CLOSED_TIME,
                                null,
                                () -> robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_CONTRACT),
                                RobotState.INIT
                        )
                )
        );

        specimenExternalTransitions.put(
                RobotState.INTAKE_REMOVE_OPPONENT_SAMPLE,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.INTAKE_REMOVE_OPPONENT_SAMPLE,
                                0,
                                null,
                                () -> {
                                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_REMOVE);
                                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_REMOVE);
                                    robot.intake.setIntakeTurretPosition(IntakeConstants.TURRET_REMOVE);
                                },
                                RobotState.INTAKE_RELEASE_OPPONENT_SAMPLE
                        )
                )
        );

        specimenExternalTransitions.put(
                RobotState.INTAKE_RELEASE_OPPONENT_SAMPLE,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.INTAKE_RELEASE_OPPONENT_SAMPLE,
                                IntakeConstants.SAMPLE_REMOVE_TIME,
                                null,
                                () -> {
                                    robot.intake.setIntakeClawPosition(IntakeConstants.CLAW_OPEN);
                                },
                                RobotState.INTAKE_REMOVE_RESET_TURRET
                        )
                )
        );

        specimenExternalTransitions.put(
                RobotState.INTAKE_REMOVE_RESET_TURRET,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.INTAKE_REMOVE_RESET_TURRET,
                                IntakeConstants.CLAW_CLOSED_TIME,
                                null,
                                () -> {
                                    robot.intake.setIntakeTurretPosition(IntakeConstants.TURRET_MIDDLE);
                                },
                                RobotState.INTAKE_REMOVE_RESET
                        )
                )
        );

        specimenExternalTransitions.put(
                RobotState.INTAKE_REMOVE_RESET,
                Collections.singletonList(
                        new StateTransition(
                                RobotState.INTAKE_REMOVE_RESET,
                                200,
                                null,
                                null,
                                RobotState.SPECIMEN_INTAKE_TARGET
                        )
                )
        );
    }

    // Performance optimization: Convert HashMaps to arrays for O(1) access
    private void convertTransitionsToArrays() {
        // Convert sample transitions
        for (Map.Entry<RobotState, List<StateTransition>> entry : sampleTransitions.entrySet()) {
            int index = entry.getKey().ordinal();
            List<StateTransition> transitions = entry.getValue();
            sampleTransitionArray[index] = transitions.toArray(new StateTransition[0]);
        }

        // Convert specimen transitions
        for (Map.Entry<RobotState, List<StateTransition>> entry : specimenTransitions.entrySet()) {
            int index = entry.getKey().ordinal();
            List<StateTransition> transitions = entry.getValue();
            specimenTransitionArray[index] = transitions.toArray(new StateTransition[0]);
        }

        // Convert specimen external transitions
        for (Map.Entry<RobotState, List<StateTransition>> entry : specimenExternalTransitions.entrySet()) {
            int index = entry.getKey().ordinal();
            List<StateTransition> transitions = entry.getValue();
            specimenExternalTransitionArray[index] = transitions.toArray(new StateTransition[0]);
        }
    }

    /**
     * Unified state transition processing method to eliminate code duplication.
     * Handles both regular states and external states with appropriate timers and state change methods.
     *
     * @param transitions Array of state transitions to process
     * @param isExternalState Whether this is processing external state (affects timer and state change method)
     * @param isSpecimenExternal Whether this is specimen external state (affects debug behavior)
     */
    private void processStateTransitions(StateTransition[] transitions, boolean isExternalState, boolean isSpecimenExternal) {
        if (transitions == null) return;

        // Use appropriate timer based on state type
        double currentTimeMs = isExternalState ? externalStateTimer.milliseconds() : stateTimer.milliseconds();

        if (ControlConstants.STATE_MACHINE_DEBUG_MODE) {
            // Special debug logic for specimen external state (multi-choice handling)
            if (isSpecimenExternal) {
                if (transitions.length > 1) {
                    // Multi-choice state: act like normal (use real button/condition logic, not debug button)
                    for (StateTransition t : transitions) {
                        if (t.condition != null && t.condition.getAsBoolean()) {
                            if (t.action != null) t.action.run();
                            if (t.nextState != null) changeExternalState(t.nextState);
                            break;
                        }
                    }
                } else {
                    // Single transition: keep debug step on A
                    if (buttonDetector.aPressed(gamepad)) {
                        StateTransition t = transitions[0];
                        if (currentTimeMs >= t.waitTimeMs) {
                            if (t.action != null) t.action.run();
                            if (t.nextState != null) changeExternalState(t.nextState);
                        }
                    }
                }
            } else {
                // Standard debug logic for sample and specimen states
                if (buttonDetector.bPressed(gamepad)) {
                    for (StateTransition t : transitions) {
                        if (currentTimeMs >= t.waitTimeMs) {
                            if (t.action != null) {
                                t.action.run();
                            }
                            // executedOnceActions not needed, since debug means every loop action
                            if (t.nextState != null) {
                                if (isExternalState) {
                                    changeExternalState(t.nextState);
                                } else {
                                    changeState(t.nextState);
                                }
                            }
                            break;
                        }
                    }
                }
            }
        } else {
            // Normal (non-debug) processing with end delay support
            for (StateTransition t : transitions) {
                boolean actionRun = executedOnceActions.contains(t);
                boolean isWaitingForEndDelay = waitingForEndDelay.contains(t);

                // Check if we're waiting for end delay to complete
                if (isWaitingForEndDelay) {
                    ElapsedTime endTimer = endDelayTimers.get(t);
                    if (endTimer != null && endTimer.milliseconds() >= t.endDelayMs) {
                        // End delay completed, transition to next state
                        if (ControlConstants.STATE_MACHINE_DEBUG_MODE) {
                            RobotLog.dd("RobotStateMachine", "End delay completed: %dms, transitioning from %s to %s",
                                (long)endTimer.milliseconds(), t.currentState, t.nextState);
                        }
                        if (t.nextState != null) {
                            if (isExternalState) {
                                changeExternalState(t.nextState);
                            } else {
                                changeState(t.nextState);
                            }
                        }
                        // Clean up end delay tracking
                        waitingForEndDelay.remove(t);
                        endDelayTimers.remove(t);
                        break;
                    }
                    // Still waiting for end delay, continue to next transition
                    if (ControlConstants.STATE_MACHINE_DEBUG_MODE && endTimer != null) {
                        // Debug: Log end delay progress every 100ms
                        long elapsed = (long)endTimer.milliseconds();
                        if (elapsed % 100 < 20) { // Log roughly every 100ms
                            RobotLog.dd("RobotStateMachine", "Waiting for end delay: %dms/%dms in state %s",
                                elapsed, (long)t.endDelayMs, t.currentState);
                        }
                    }
                    continue;
                }

                // Normal transition processing
                if (currentTimeMs >= t.waitTimeMs && (t.condition == null || t.condition.getAsBoolean())) {
                    if (t.action != null && !actionRun) {
                        t.action.run();
                        executedOnceActions.add(t);
                    }

                    // Handle end delay if specified
                    if (t.endDelayMs > 0 && t.nextState != null) {
                        // Start end delay timer
                        ElapsedTime endTimer = new ElapsedTime();
                        endDelayTimers.put(t, endTimer);
                        waitingForEndDelay.add(t);
                        if (ControlConstants.STATE_MACHINE_DEBUG_MODE) {
                            RobotLog.dd("RobotStateMachine", "Starting end delay: %dms in state %s, will transition to %s",
                                (long)t.endDelayMs, t.currentState, t.nextState);
                        }
                    } else if (t.nextState != null) {
                        // No end delay, transition immediately
                        if (isExternalState) {
                            changeExternalState(t.nextState);
                        } else {
                            changeState(t.nextState);
                        }
                    }
                    break;
                }
            }
        }
    }

    private void processSampleState() {
        StateTransition[] transitions = sampleTransitionArray[currentState.ordinal()];
        processStateTransitions(transitions, false, false);
    }

    private void processSpecimenState() {
        StateTransition[] transitions = specimenTransitionArray[currentState.ordinal()];
        processStateTransitions(transitions, false, false);
    }

    private void processSpecimenExternalState() {
        StateTransition[] transitions = specimenExternalTransitionArray[externalState.ordinal()];
        processStateTransitions(transitions, true, true);
    }

    private void scheduleOuttakeReset(long delayMs) {
        scheduleOuttakeReset = true;
        outtakeResetTimer.reset();
        outtakeResetDelay = delayMs;
    }

    private void handleOuttakeResetIfNeeded() {
        if (scheduleOuttakeReset) {
            // Check if slide has reached minimum position via limit switch (faster reset)
            // or if timer has expired (fallback for safety)
            boolean limitSwitchTriggered = robot.outtake.isSlideAtMinPosition();
            boolean timerExpired = outtakeResetTimer.milliseconds() > outtakeResetDelay;

            if (limitSwitchTriggered || timerExpired) {
                robot.outtake.resetSlide();
                outtakeResetDone = true;
                scheduleOuttakeReset = false;
            }
        }
    }

    private void scheduleIntakeSlideReset(long delayMs) {
        scheduleIntakeSlideReset = true;
        intakeSlideResetTimer.reset();
        intakeSlideResetDelay = delayMs;
        intakeSlideResetDone = false;
    }

    /**
     * Public method to schedule intake slide auto-return from external classes
     * Uses the existing scheduleIntakeSlideReset system
     * @param delayMs Delay in milliseconds before auto-return to minimum position
     */
    public void scheduleIntakeSlideAutoReturn(long delayMs) {
        scheduleIntakeSlideReset(delayMs);
    }

    private void handleIntakeSlideResetIfNeeded() {
        if (scheduleIntakeSlideReset) {
            // Check if slide has reached minimum position via limit switch (faster reset)
            // or if timer has expired (fallback for safety)
            boolean limitSwitchTriggered = robot.intake.isSlideAtMinPosition();
            boolean timerExpired = intakeSlideResetTimer.milliseconds() > intakeSlideResetDelay;

            if (limitSwitchTriggered || timerExpired) {
                isIntaking = false;
                robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MIN);
                intakeSlideResetDone = true;
                scheduleIntakeSlideReset = false;
            }
        }
    }

    /**
     * Update the state machine when in Sample mode (modularized)
     */
    private void updateSampleModes() {
        if (currentMode != RobotMode.SAMPLE) {
            return; // Only process if we're in Sample mode
        }
        processSampleState();
    }

    public void setGamepad(Gamepad gamepad) {
        this.gamepad = gamepad;
    }

    public void setButtonDetector(ButtonEdgeDetector buttonDetector) {
        this.buttonDetector = buttonDetector;
    }

    /**
     * Set the team color from teleop (optimized string storage)
     */
    public void setTeamColor(Object teamColor) {
        this.teamColorName = teamColor != null ? teamColor.toString() : null;
    }

    public void changeState(RobotState newState) {
        if (currentState != newState) {
            currentState = newState;
            stateTimer.reset();
            executedOnceActions.clear();
            // Clean up end delay tracking when changing states
            waitingForEndDelay.clear();
            endDelayTimers.clear();
            RobotLog.dd("RobotStateMachine", "State: %s", newState);
        }
    }

    public RobotState getCurrentState() {
        return currentState;
    }

    public RobotMode getCurrentMode() {
        return currentMode;
    }

    public RobotState getExternalState() {
        return externalState;
    }

    public double getStateElapsedTime() {
        return stateTimer.milliseconds();
    }

    public void changeExternalState(RobotState newState) {
        if (externalState != newState) {
            externalState = newState;
            externalStateTimer.reset();
            executedOnceActions.clear();
            // Clean up end delay tracking when changing external states
            waitingForEndDelay.clear();
            endDelayTimers.clear();
            RobotLog.dd("RobotStateMachine", "External: %s", newState);
        }
    }

    public void switchMode(RobotMode newMode) {
        if (currentMode == newMode) return;

        // Prevent mode change if outtake reset is ongoing
        if (scheduleOuttakeReset && !outtakeResetDone) {
            telemetry.addData("Mode Switch Blocked", "Outtake reset in progress");
            return;
        }

        // Prevent mode change during timer-based states (sensitive timing)
//        if (isInTimerBasedState()) {
//            String blockingState = getBlockingStateInfo();
//            telemetry.addData("Mode Switch Blocked", "Timer-based state: " + blockingState);
//            return;
//        }

        currentMode = newMode;
        RobotLog.dd("RobotStateMachine", "Mode: %s", newMode);

//        // Reset states based on new mode
//        if (newMode == RobotMode.SPECIMEN) {
//            switch (currentState){
//                case SAMPLE_OUTTAKE_ELBOW_BASKET:
//                robot.outtake.setClawPosition(OuttakeConstants.CLAW_OPEN);
//                robot.outtake.setSlidePosition(OuttakeConstants.SLIDE_MIN);
//                robot.outtake.setElbowPosition(OuttakeConstants.ELBOW_REST);
//                robot.outtake.setWristPosition(OuttakeConstants.ELBOW_REST);
//                outtakeResetDone = false;
//                isIntaking = false;
//                scheduleOuttakeReset(ControlConstants.OUTTAKE_RESET_DELAY_LONG_MS);
//                changeExternalState(RobotState.INIT);
//                changeState(RobotState.HOLD);
//                break;
//
//
//
//                default:
//                    changeExternalState(RobotState.INIT);
//                    changeState(RobotState.HOLD);
//                    isIntaking = false;
//                break;
//            }
//
//        } else if (newMode == RobotMode.SAMPLE) {
//            switch (currentState) {
//                case SPECIMEN_OUTTAKE_CHECK:
//                    robot.outtake.setClawPosition(OuttakeConstants.CLAW_OPEN);
//                    outtakeResetDone = false;
//                    scheduleOuttakeReset(ControlConstants.OUTTAKE_RESET_DELAY_MS);
//                    changeExternalState(RobotState.INIT);
//                    changeState(RobotState.HOLD);
//
//                    break;
//
//                default:
//                    changeExternalState(RobotState.INIT);
//                    changeState(RobotState.INIT);
//                    break;
//            }
//
//        }

        changeExternalState(RobotState.INIT);
        changeState(RobotState.HOLD);
        isIntaking = false;

        if (gamepad != null) gamepad.rumble(ControlConstants.MODE_SWITCH_RUMBLE_DURATION_MS);
    }



    public void update() {
        try {
            if (!validateInputs()) return;

            long currentTimeMs = System.currentTimeMillis();
            handleSystemUpdates(currentTimeMs);
            processGamepadInputs();
            executeStateMachine();

        } catch (Exception e) {
            handleUpdateException(e);
        }
    }

    /**
     * Validate that required inputs are available
     * @return true if update should continue
     */
    private boolean validateInputs() {
        return gamepad != null && buttonDetector != null;
    }

    /**
     * Handle system-level updates that run every cycle
     * @param currentTimeMs Current time in milliseconds
     */
    private void handleSystemUpdates(long currentTimeMs) {
        handleOuttakeResetIfNeeded();
        handleIntakeSlideResetIfNeeded();
        handleStateSpecificColorChecking(currentTimeMs);
    }

    /**
     * Process all gamepad inputs and button presses
     */
    private void processGamepadInputs() {
        handleToggleButtons();
        handleModeSwitch();
        handleSlideControls();
    }

    /**
     * Handle toggle buttons (X for hold/init, left bumper for init)
     */
    private void handleToggleButtons() {
        // X button for hold/init toggle using shared edge detection
        if (buttonDetector.xPressed(gamepad)) {
            if ((currentState != RobotState.HOLD || currentState == RobotState.SPECIMEN_OUTTAKE_CLAW_CLOSE) && currentMode == RobotMode.SPECIMEN) {
                changeState(RobotState.HOLD);
            } else {
                changeState(RobotState.INIT);
            }
        }


        if (currentState == RobotState.HOLD && buttonDetector.aPressed(gamepad)) {
            changeState(RobotState.INIT);
        }

//        // Auto-transition to HOLD when slide is retracted in specimen mode
//        if (robot.intake.getIntakeSlidePosition() <= IntakeConstants.SLIDE_MAX_EXTENSION && currentMode == RobotMode.SPECIMEN) {
//            changeState(RobotState.HOLD);
//        }

        // Left bumper to force INIT from HOLD
        if (currentState == RobotState.HOLD && buttonDetector.leftBumperPressed(gamepad)) {
            changeState(RobotState.INIT);
        }
    }

    /**
     * Handle mode switching between SAMPLE and SPECIMEN
     */
    private void handleModeSwitch() {
//        if (buttonDetector.startPressed(gamepad)) {
//            switchMode(currentMode == RobotMode.SAMPLE ? RobotMode.SPECIMEN : RobotMode.SAMPLE);
//        }
//
//        // Early exit if start button is held (prevents other inputs)
//        if (gamepad.start) {
//            return;
//        }
    }

    /**
     * Handle slide position controls (D-pad up/down)
     */
    private void handleSlideControls() {
        if (buttonDetector.dpadDownPressed(gamepad)) {
            if (currentState != RobotState.SAMPLE_INTAKE_CLOSE && currentState != RobotState.SAMPLE_INTAKE_CLAW_CLOSE && currentState != RobotState.SAMPLE_INTAKE_GRAB) {
                robot.intake.setIntakeSlidePositionWithAutoReturn(IntakeConstants.SLIDE_HOLD);
                robot.intake.setIntakeTurretPosition(IntakeConstants.TURRET_MIDDLE);
                handleSlidePositionStateTransitions(true); // true = retracting
            }
        }

        if (buttonDetector.dpadUpPressed(gamepad)) {
            robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MAX);
            robot.intake.setIntakeTurretPosition(IntakeConstants.TURRET_MIDDLE);
            handleSlidePositionStateTransitions(false); // false = extending
        }
    }

    /**
     * Handle state transitions based on slide position changes
     * @param isRetracting true if slide is retracting, false if extending
     */
    private void handleSlidePositionStateTransitions(boolean isRetracting) {
        if (isRetracting) {
            // D-pad down: retract slide and transition states
            if (currentMode == RobotMode.SPECIMEN) {
                if (externalState == RobotState.SPECIMEN_INTAKE_POSITION_CHECK) {
                    changeExternalState(RobotState.INIT);
                }
            } else if (currentMode == RobotMode.SAMPLE) {
                if (currentState == RobotState.SAMPLE_INTAKE_POSITION_CHECK) {
                    changeState(RobotState.INIT);
                }
            }
        } else {
            // D-pad up: extend slide and transition states
            if (currentMode == RobotMode.SAMPLE && currentState == RobotState.SAMPLE_INTAKE_TARGET_CHECK) {
                changeState(RobotState.SAMPLE_INTAKE_TARGET);
            } else if (currentMode == RobotMode.SPECIMEN && externalState == RobotState.SPECIMEN_INTAKE_TARGET_CHECK) {
                changeExternalState(RobotState.SPECIMEN_INTAKE_TARGET);
            }
        }
    }

    /**
     * Execute the appropriate state machine based on current mode
     */
    private void executeStateMachine() {
        if (currentMode == RobotMode.SAMPLE) {
            processSampleState();
        } else if (currentMode == RobotMode.SPECIMEN) {
            processSpecimenExternalState();
            processSpecimenState();
        }
    }

    /**
     * Handle exceptions that occur during update with proper recovery
     * @param e The exception that occurred
     */
    private void handleUpdateException(Exception e) {
        // Log the exception with context
        telemetry.addData("ERROR", "State machine exception: " + e.getMessage());
        RobotLog.ee("RobotStateMachine", e, "Exception in update()");

        // Attempt graceful recovery
        try {
            changeState(RobotState.INIT);
            telemetry.addData("RECOVERY", "Reset to INIT state");
        } catch (Exception recoveryException) {
            // If recovery fails, log critical error but don't crash
            telemetry.addData("CRITICAL ERROR", "Recovery failed: " + recoveryException.getMessage());
            RobotLog.ee("RobotStateMachine", recoveryException, "Failed to recover from exception");
        }
    }

    // Mode switch debounce

    private SampleColor getDirectSampleColor() {
        return robot.intake.getSampleColor();
    }

    private SampleColor getDirectOuttakeSampleColor() {
        return robot.outtake.getSampleColor();
    }

    /**
     * Check if the current state is timer-based and should block mode switching
     * @return true if mode switching should be blocked due to timer-based state
     */
    private boolean isInTimerBasedState() {
        // Check if we're waiting for an end delay (most sensitive timing)
        if (!waitingForEndDelay.isEmpty()) {
            return true;
        }

        // Check current state for timer-based blocking
        boolean currentStateBlocked = isStateTimerBlocking(currentState, false);

        // Check external state for timer-based blocking (specimen mode)
        boolean externalStateBlocked = isStateTimerBlocking(externalState, true);

        return currentStateBlocked || externalStateBlocked;
    }

    /**
     * Check if a specific state should block mode switching due to active timers
     * @param state The state to check
     * @param isExternalState Whether this is an external state (affects timer selection)
     * @return true if mode switching should be blocked due to active timer
     */
    private boolean isStateTimerBlocking(RobotState state, boolean isExternalState) {
        if (state == null) return false;

        // Get the appropriate timer and transitions for this state
        double currentTimeMs = isExternalState ? externalStateTimer.milliseconds() : stateTimer.milliseconds();
        StateTransition[] transitions = getTransitionsForState(state, isExternalState);

        if (transitions == null || transitions.length == 0) return false;

        // Check if any transition in this state has an active timer or immediate action
        for (StateTransition transition : transitions) {
            if (transition.waitTimeMs > 0) {
                // Timer-based state: block mode switching only if timer hasn't elapsed
                if (currentTimeMs < transition.waitTimeMs) {
                    return true; // Timer still running, block mode switching
                }
                // Timer has elapsed - check if it has gamepad input
                if (transition.condition != null) {
                    // Has gamepad input after timer - allow mode switching
                    return false;
                }
                // Pure timer state (no gamepad input) - continue blocking
                return true;
            } else if (transition.waitTimeMs == 0 && transition.condition == null && transition.action != null) {
                // Immediate action state (0 timer, no condition, has action) - always block
                // These states execute hardware actions immediately and are sensitive
                return true;
            }
        }

        return false; // No timers in this state
    }

    /**
     * Get detailed information about which state is blocking mode switching
     * @return String describing the blocking state and timing info
     */
    private String getBlockingStateInfo() {
        // Check for end delay first (highest priority)
        if (!waitingForEndDelay.isEmpty()) {
            return "End delay active";
        }

        // Check current state
        if (isStateTimerBlocking(currentState, false)) {
            double currentTimeMs = stateTimer.milliseconds();
            StateTransition[] transitions = getTransitionsForState(currentState, false);
            if (transitions != null && transitions.length > 0) {
                for (StateTransition transition : transitions) {
                    if (transition.waitTimeMs > 0) {
                        double remaining = transition.waitTimeMs - currentTimeMs;
                        if (remaining > 0) {
                            return String.format("%s (%.0fms remaining)", currentState, remaining);
                        } else if (transition.condition == null) {
                            return String.format("%s (pure timer)", currentState);
                        }
                    } else if (transition.waitTimeMs == 0 && transition.condition == null && transition.action != null) {
                        return String.format("%s (immediate action)", currentState);
                    }
                }
            }
            return currentState.toString();
        }

        // Check external state
        if (isStateTimerBlocking(externalState, true)) {
            double currentTimeMs = externalStateTimer.milliseconds();
            StateTransition[] transitions = getTransitionsForState(externalState, true);
            if (transitions != null && transitions.length > 0) {
                for (StateTransition transition : transitions) {
                    if (transition.waitTimeMs > 0) {
                        double remaining = transition.waitTimeMs - currentTimeMs;
                        if (remaining > 0) {
                            return String.format("%s (%.0fms remaining)", externalState, remaining);
                        } else if (transition.condition == null) {
                            return String.format("%s (pure timer)", externalState);
                        }
                    } else if (transition.waitTimeMs == 0 && transition.condition == null && transition.action != null) {
                        return String.format("%s (immediate action)", externalState);
                    }
                }
            }
            return externalState.toString();
        }

        return "Unknown blocking state";
    }

    /**
     * Get transitions for a specific state
     * @param state The state to get transitions for
     * @param isExternalState Whether this is an external state
     * @return Array of transitions for the state, or null if not found
     */
    private StateTransition[] getTransitionsForState(RobotState state, boolean isExternalState) {
        if (state == null) return null;

        try {
            if (isExternalState) {
                return specimenExternalTransitionArray[state.ordinal()];
            } else if (currentMode == RobotMode.SPECIMEN) {
                return specimenTransitionArray[state.ordinal()];
            } else {
                return sampleTransitionArray[state.ordinal()];
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return null;
        }
    }

    /**
     * Handle state-specific color checking (only during intake check states for 1 second)
     */
    private void handleStateSpecificColorChecking(long currentTimeMs) {
        // Check if we're in a color check state
        boolean shouldBeInColorCheckState = (currentState == RobotState.SAMPLE_INTAKE_CHECK_SAMPLE) ||
                                          (externalState == RobotState.SPECIMEN_INTAKE_CHECK_SAMPLE);

        // Start color checking when entering a check state
        if (shouldBeInColorCheckState && !isInColorCheckState) {
            isInColorCheckState = true;
            colorCheckStateStartTime = currentTimeMs;
            colorCheckResult = SampleColor.NONE;
            RobotLog.dd("RobotStateMachine", "Started color checking for intake state");
        }

        // Stop color checking when leaving check state or after duration
        if (isInColorCheckState && (!shouldBeInColorCheckState ||
            (currentTimeMs - colorCheckStateStartTime) > ControlConstants.COLOR_CHECK_DURATION_MS)) {
            isInColorCheckState = false;
            RobotLog.dd("RobotStateMachine", "Stopped color checking, final result: %s", colorCheckResult);
        }

        // Only check color sensor during the 1-second window in check states
        if (isInColorCheckState && (currentTimeMs - colorCheckStateStartTime) <= ControlConstants.COLOR_CHECK_DURATION_MS) {
            SampleColor currentSample = getDirectSampleColor();

            // Update result if we detect a sample (prioritize non-NONE results)
            if (currentSample != SampleColor.NONE) {
                colorCheckResult = currentSample;

                // Trigger feedback for new sample detection
                if (currentSample != lastDetectedSample) {
                    lastDetectedSample = currentSample;
                    lastSampleFeedbackTime = currentTimeMs;
                    isBlinkingForSample = true;
                    newSampleDetected = true;
                    intakeSample = currentSample;
                    RobotLog.dd("RobotStateMachine", "Sample detected during intake check: %s", currentSample);
                }
            }
        }

        // Stop blinking after feedback duration (configurable)
        if (isBlinkingForSample && currentTimeMs - lastSampleFeedbackTime >= ControlConstants.SAMPLE_FEEDBACK_DURATION_MS) {
            isBlinkingForSample = false;
        }
    }

    /**
     * Get the last detected sample color
     */
    public SampleColor getLastDetectedSample() {
        return lastDetectedSample;
    }

    /**
     * Check if sample feedback is currently active (for hub LED blinking)
     */
    public boolean isSampleFeedbackActive() {
        return isBlinkingForSample;
    }

    /**
     * Check if a new sample was just detected (for rumble feedback)
     */
    public boolean isNewSampleDetected() {
        if (newSampleDetected) {
            newSampleDetected = false; // Reset flag after reading
            return true;
        }
        return false;
    }

    /**
     * Get the time since last sample feedback started
     */
    public long getTimeSinceLastSampleFeedback(long currentTimeMs) {
        return currentTimeMs - lastSampleFeedbackTime;
    }

    /**
     * Get the color check result for auto-advance decisions (only valid during intake check states)
     */
    private SampleColor getColorCheckResult() {
        return isInColorCheckState ? colorCheckResult : SampleColor.NONE;
    }

    /**
     * Formal color checking with distance validation for intake system
     * This provides more robust color detection similar to the outtake distance checking
     * @return ColorCheckResult containing both the color and validation status
     */
    private ColorCheckResult getFormalColorCheckResult() {
        if (!isInColorCheckState) {
            return new ColorCheckResult(SampleColor.NONE, false, -1);
        }

        // Get distance for validation
        double distance = robot.intake.getIntakeDistance();
        boolean isValidDistance = distance > 0 && distance <= ControlConstants.COLOR_DETECTION_MAX_DISTANCE_CM;

        // Only return valid color if distance is acceptable
        SampleColor validatedColor = isValidDistance ? colorCheckResult : SampleColor.NONE;

        return new ColorCheckResult(validatedColor, isValidDistance, distance);
    }

    /**
     * Helper class to encapsulate color check results with validation
     */
    private static class ColorCheckResult {
        final SampleColor color;
        final boolean isValidDistance;
        final double distance;

        ColorCheckResult(SampleColor color, boolean isValidDistance, double distance) {
            this.color = color;
            this.isValidDistance = isValidDistance;
            this.distance = distance;
        }

        boolean hasValidSample() {
            return isValidDistance && color != SampleColor.NONE;
        }
    }

    /**
     * Helper class to encapsulate specimen detection results with validation
     */
    private static class SpecimenDetectionResult {
        final boolean isDetected;
        final boolean isValidDistance;
        final double distance;

        SpecimenDetectionResult(boolean isDetected, boolean isValidDistance, double distance) {
            this.isDetected = isDetected;
            this.isValidDistance = isValidDistance;
            this.distance = distance;
        }

        boolean hasValidSpecimen() {
            return isValidDistance && isDetected;
        }
    }

    /**
     * Formal specimen detection with distance validation for outtake system
     * This provides consistent validation pattern similar to color checking
     * @return SpecimenDetectionResult containing detection status and validation
     */
    private SpecimenDetectionResult getFormalSpecimenDetectionResult() {
        if (robot.outtake == null) {
            return new SpecimenDetectionResult(false, false, -1);
        }

        // Get distance and detection status using proper public methods
        double distance = robot.outtake.getOuttakeDistance();
        boolean isValidDistance = distance > 0 && distance <= ControlConstants.SPECIMEN_DETECTION_DISTANCE_CM;
        boolean isDetected = robot.outtake.isSpecimenDetected();

        return new SpecimenDetectionResult(isDetected, isValidDistance, distance);
    }

    // Public interface methods
    public boolean getIsIntaking() {
        return isIntaking;
    }

    public static SampleColor getIntakeSample() {
        return intakeSample;
    }

    public static SampleColor getOuttakeSample() {
        return outtakeSample;
    }

    /**
     * Get the current sample color (for teleop telemetry)
     * Returns cached result during color check states, otherwise reads directly
     */
    public SampleColor getBatchedSampleColor() {
        if (isInColorCheckState) {
            return colorCheckResult;
        }
        return robot.intake.getSampleColor();
    }

    /**
     * Optimized color checking methods using cached team color string
     */
    private boolean isAllianceOrNeutralSample(SampleColor sampleColor) {
        if (sampleColor == SampleColor.YELLOW) return true;
        if (teamColorName == null) return false;

        return (sampleColor == SampleColor.RED && "RED".equals(teamColorName)) ||
               (sampleColor == SampleColor.BLUE && "BLUE".equals(teamColorName));
    }

    private boolean isAllianceSample(SampleColor sampleColor) {
        if (teamColorName == null) return false;
        return (sampleColor == SampleColor.RED && "RED".equals(teamColorName)) ||
               (sampleColor == SampleColor.BLUE && "BLUE".equals(teamColorName));
    }

    private boolean isOpponentSample(SampleColor sampleColor) {
        if (teamColorName == null || sampleColor == SampleColor.NONE || sampleColor == SampleColor.YELLOW) {
            return false;
        }
        return (sampleColor == SampleColor.RED && "BLUE".equals(teamColorName)) ||
               (sampleColor == SampleColor.BLUE && "RED".equals(teamColorName));
    }

    public String getTeamColor() {
        return teamColorName;
    }

    /**
     * Debug helper methods
     */
    public boolean isDebugMode() {
        return ControlConstants.STATE_MACHINE_DEBUG_MODE;
    }

    public void debugLog(String message) {
        if (ControlConstants.STATE_MACHINE_DEBUG_MODE) {
            RobotLog.dd("RobotStateMachine", "DEBUG: %s", message);
        }
    }

    public String getDebugInfo() {
        return String.format("Mode: %s, State: %s, External: %s, ColorCheck: %s",
                           currentMode, currentState, externalState, colorCheckResult);
    }

    /**
     * Check if outtake reset is currently in progress
     * @return true if outtake reset is scheduled and not yet complete
     */
    public boolean isOuttakeResetInProgress() {
        return scheduleOuttakeReset && !outtakeResetDone;
    }

}
