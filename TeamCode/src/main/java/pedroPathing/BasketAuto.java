package pedroPathing;

import android.graphics.Color;

import com.pedropathing.follower.Follower;
import com.pedropathing.localization.Pose;
import com.pedropathing.pathgen.BezierCurve;
import com.pedropathing.pathgen.BezierLine;
import com.pedropathing.pathgen.Path;
import com.pedropathing.pathgen.PathChain;
import com.pedropathing.pathgen.Point;
import com.pedropathing.util.Constants;
import com.pedropathing.util.Timer;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.Gamepad;

import pedroPathing.constants.BasketAutoConstant;
import pedroPathing.constants.FConstants;
import pedroPathing.constants.IntakeConstants;
import pedroPathing.constants.LConstants;
import pedroPathing.constants.OuttakeConstants;
import pedroPathing.hardware.RobotHardware;
import pedroPathing.robot_state.RobotMode;
import pedroPathing.robot_state.RobotState;
import pedroPathing.robot_state.RobotStateMachine;
import pedroPathing.util.ButtonEdgeDetector;

/**
 * This is an example auto that showcases movement and control of two servos autonomously.
 * It is a 0+4 (Specimen + Sample) bucket auto. It scores a neutral preload and then pickups 3 samples from the ground and scores them before parking.
 * There are examples of different ways to build paths.
 * A path progression method has been created and can advance based on time, position, or other factors.
 *
 * @author Baron Henderson - 20077 The Indubitables
 * @version 2.0, 11/28/2024
 */

@Autonomous(name = "Basket Aut")
public class BasketAuto extends OpMode {

    private Follower follower;
    private Timer pathTimer, opmodeTimer;

    // Robot hardware and state machine
    private final RobotHardware robot = new RobotHardware(this);
    private RobotStateMachine stateMachine;
    private ButtonEdgeDetector buttonDetector = new ButtonEdgeDetector();

    // Team color for autonomous
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

    // Set team color for this autonomous (change as needed)
    private final TeamColor teamColor = TeamColor.BLUE;

    /** This is the variable where we store the state of our auto.
     * It is used by the pathUpdate method. */
    private int pathState;

    /** Simple crash-proof timing - no complex wait system needed */


    /* Create and Define Poses + Paths
     * Poses are built with three constructors: x, y, and heading (in Radians).
     * Pedro uses 0 - 144 for x and y, with 0, 0 being on the bottom left.
     * (For Into the Deep, this would be Blue Observation Zone (0,0) to Red Observation Zone (144,144).)
     * Even though Pedro uses a different coordinate system than RR, you can convert any roadrunner pose by adding +72 both the x and y.
     * This visualizer is very easy to use to find and create paths/pathchains/poses: <https://pedro-path-generator.vercel.app/>
     * Lets assume our robot is 18 by 18 inches
     * Lets assume the Robot is facing the human player and we want to score in the bucket */

    /** Start Pose of our robot */
    private final Pose startPose = new Pose(BasketAutoConstant.START_X, BasketAutoConstant.START_Y, Math.toRadians(BasketAutoConstant.START_HEADING_DEG));

    /** Scoring Pose of our robot. It is facing the submersible at a -45 degree (315 degree) angle. */
    private final Pose scorePose = new Pose(BasketAutoConstant.SCORE_X, BasketAutoConstant.SCORE_Y, Math.toRadians(BasketAutoConstant.SCORE_HEADING_DEG));

    /** Lowest (First) Sample from the Spike Mark */
    private final Pose pickup1Pose = new Pose(BasketAutoConstant.PICKUP1_X, BasketAutoConstant.PICKUP1_Y, Math.toRadians(BasketAutoConstant.PICKUP1_HEADING_DEG));

    /** Middle (Second) Sample from the Spike Mark */
    private final Pose pickup2Pose = new Pose(BasketAutoConstant.PICKUP2_X, BasketAutoConstant.PICKUP2_Y, Math.toRadians(BasketAutoConstant.PICKUP2_HEADING_DEG));

    /** Highest (Third) Sample from the Spike Mark */
    private final Pose pickup3Pose = new Pose(BasketAutoConstant.PICKUP3_X, BasketAutoConstant.PICKUP3_Y, Math.toRadians(BasketAutoConstant.PICKUP3_HEADING_DEG));

    /** Park Pose for our robot, after we do all of the scoring. */
    private final Pose parkPose = new Pose(BasketAutoConstant.PARK_X, BasketAutoConstant.PARK_Y, Math.toRadians(BasketAutoConstant.PARK_HEADING_DEG));

    /** Park Control Pose for our robot, this is used to manipulate the bezier curve that we will create for the parking.
     * The Robot will not go to this pose, it is used a control point for our bezier curve. */
    private final Pose parkControlPose = new Pose(BasketAutoConstant.PARK_CONTROL_X, BasketAutoConstant.PARK_CONTROL_Y, Math.toRadians(BasketAutoConstant.PARK_CONTROL_HEADING_DEG));

    /* These are our Paths and PathChains that we will define in buildPaths() */
    private Path scorePreload, park, returnToStart;
    private PathChain grabPickup1, grabPickup2, grabPickup3, scorePickup1, scorePickup2, scorePickup3;

    /** Build the paths for the auto (adds, for example, constant/linear headings while doing paths)
     * It is necessary to do this so that all the paths are built before the auto starts. **/
    public void buildPaths() {

        /* There are two major types of paths components: BezierCurves and BezierLines.
         *    * BezierCurves are curved, and require >= 3 points. There are the start and end points, and the control points.
         *    - Control points manipulate the curve between the start and end points.
         *    - A good visualizer for this is [this](https://pedro-path-generator.vercel.app/).
         *    * BezierLines are straight, and require 2 points. There are the start and end points.
         * Paths have can have heading interpolation: Constant, Linear, or Tangential
         *    * Linear heading interpolation:
         *    - Pedro will slowly change the heading of the robot from the startHeading to the endHeading over the course of the entire path.
         *    * Constant Heading Interpolation:
         *    - Pedro will maintain one heading throughout the entire path.
         *    * Tangential Heading Interpolation:
         *    - Pedro will follows the angle of the path such that the robot is always driving forward when it follows the path.
         * PathChains hold Path(s) within it and are able to hold their end point, meaning that they will holdPoint until another path is followed.
         * Here is a explanation of the difference between Paths and PathChains <https://pedropathing.com/commonissues/pathtopathchain.html> */

        /* This is our scorePreload path. We are using a BezierLine, which is a straight line. */
        scorePreload = new Path(new BezierLine(new Point(startPose), new Point(scorePose)));
        scorePreload.setLinearHeadingInterpolation(startPose.getHeading(), scorePose.getHeading());

        /* Here is an example for Constant Interpolation
        scorePreload.setConstantInterpolation(startPose.getHeading()); */

        /* This is our grabPickup1 PathChain. We are using a single path with a BezierLine, which is a straight line. */
        Path grabPickup1Path = new Path(new BezierLine(new Point(scorePose), new Point(pickup1Pose)));
        grabPickup1Path.setLinearHeadingInterpolation(scorePose.getHeading(), pickup1Pose.getHeading());
        grabPickup1 = new PathChain(grabPickup1Path);

        /* This is our scorePickup1 PathChain. We are using a single path with a BezierLine, which is a straight line. */
        Path scorePickup1Path = new Path(new BezierLine(new Point(pickup1Pose), new Point(scorePose)));
        scorePickup1Path.setLinearHeadingInterpolation(pickup1Pose.getHeading(), scorePose.getHeading());
        scorePickup1 = new PathChain(scorePickup1Path);

        /* This is our grabPickup2 PathChain. We are using a single path with a BezierLine, which is a straight line. */
        Path grabPickup2Path = new Path(new BezierLine(new Point(scorePose), new Point(pickup2Pose)));
        grabPickup2Path.setLinearHeadingInterpolation(scorePose.getHeading(), pickup2Pose.getHeading());
        grabPickup2 = new PathChain(grabPickup2Path);

        /* This is our scorePickup2 PathChain. We are using a single path with a BezierLine, which is a straight line. */
        Path scorePickup2Path = new Path(new BezierLine(new Point(pickup2Pose), new Point(scorePose)));
        scorePickup2Path.setLinearHeadingInterpolation(pickup2Pose.getHeading(), scorePose.getHeading());
        scorePickup2 = new PathChain(scorePickup2Path);

        /* This is our grabPickup3 PathChain. We are using a single path with a BezierLine, which is a straight line. */
        Path grabPickup3Path = new Path(new BezierLine(new Point(scorePose), new Point(pickup3Pose)));
        grabPickup3Path.setLinearHeadingInterpolation(scorePose.getHeading(), pickup3Pose.getHeading());
        grabPickup3 = new PathChain(grabPickup3Path);

        /* This is our scorePickup3 PathChain. We are using a single path with a BezierLine, which is a straight line. */
        Path scorePickup3Path = new Path(new BezierLine(new Point(pickup3Pose), new Point(scorePose)));
        scorePickup3Path.setLinearHeadingInterpolation(pickup3Pose.getHeading(), scorePose.getHeading());
        scorePickup3 = new PathChain(scorePickup3Path);

        /* This is our park path. We are using a BezierCurve with 3 points, which is a curved line that is curved based off of the control point */
        park = new Path(new BezierCurve(new Point(scorePose), /* Control Point */ new Point(parkControlPose), new Point(parkPose)));
        park.setLinearHeadingInterpolation(scorePose.getHeading(), parkPose.getHeading());

        /* This is our return to start path. We are using a BezierLine to go straight back to starting position */
        returnToStart = new Path(new BezierLine(new Point(scorePose), new Point(startPose)));
        returnToStart.setLinearHeadingInterpolation(scorePose.getHeading(), startPose.getHeading());
    }

    /** This switch is called continuously and runs the pathing, at certain points, it triggers the action state.
     * Everytime the switch changes case, it will reset the timer. (This is because of the setPathState() method)
     * The followPath() function sets the follower to run the specific path, but does NOT wait for it to finish before moving on. */
    public void autonomousPathUpdate() {
        // If already in error state, don't execute any path logic
        if(pathState == -1) {
            return;
        }

        try {
            switch (pathState) {
            case 0:
                robot.outtake.setOuttakeSlidePosition(OuttakeConstants.OUTTAKE_SLIDE_BASKET);
                stateMachine.changeState(RobotState.SAMPLE_OUTTAKE_SLIDES_EXTEND);
                follower.followPath(scorePreload);
                setPathState(1);
                break;

            case 1:
                /* Wait for outtake sequence to complete, then move to pickup with proper timing */
                if(stateMachine != null && stateMachine.getCurrentState() == RobotState.SAMPLE_OUTTAKE_ELBOW_BASKET) {
                    /* Wait for claw release delay, then open claw */
                    if(pathTimerElapsed(BasketAutoConstant.OUTTAKE_CLAW_RELEASE / 1000.0)) {
                        robot.outtake.setOuttakeClawPosition(OuttakeConstants.CLAW_OPEN);

                        /* Wait additional time for claw to open, then change state and start path */
                        if(pathTimerElapsed((BasketAutoConstant.OUTTAKE_CLAW_RELEASE + OuttakeConstants.OUTTAKE_CLAW_CLOSED_TIME) / 1000.0)) {
                            stateMachine.changeState(RobotState.SAMPLE_OUTTAKE_DUMP);
                            follower.followPath(grabPickup1, true);
                            setPathState(2);
                        }
                    }
                }
                break;
            case 2:
                /* This case checks the robot's position and will wait until the robot position is close (1 inch away) from the pickup1Pose's position */
                if(!follower.isBusy()) {
                    // Start turret movement and extend slides immediately
                    rotateTurret(BasketAutoConstant.SAMPLE1_TURRET_POS);
                    rotateClawWrist(BasketAutoConstant.SAMPLE1_WRIST_POS);
                    robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MAX);
                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_PREP);
                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_PREP);


                    if(pathTimerElapsed(BasketAutoConstant.INTAKE_SLIDE_WAIT_TIME / 1000.0)) {
                        stateMachine.changeState(RobotState.SAMPLE_INTAKE_GRAB);
                        if (stateMachine.getCurrentState() == RobotState.SAMPLE_INTAKE_CHECK_SAMPLE){
                            stateMachine.changeState(RobotState.SAMPLE_INTAKE_WRIST_MOVE);
                            setPathState(9);
                        }
                    }
                }
                break;
            case 3:
                if(stateMachine.getCurrentState() == RobotState.COMPLETE_INTAKE) {
                    robot.outtake.setOuttakeSlidePosition(OuttakeConstants.OUTTAKE_SLIDE_BASKET);
                    stateMachine.changeState(RobotState.SAMPLE_OUTTAKE_SLIDES_EXTEND);

                    follower.followPath(scorePickup1,true);
                    setPathState(4);
                }
                break;
            case 4:
                if(stateMachine != null && stateMachine.getCurrentState() == RobotState.SAMPLE_OUTTAKE_ELBOW_BASKET) {
                    if(pathTimerElapsed(BasketAutoConstant.OUTTAKE_CLAW_RELEASE / 1000.0)) {
                        robot.outtake.setOuttakeClawPosition(OuttakeConstants.CLAW_OPEN);

                        if(pathTimerElapsed((BasketAutoConstant.OUTTAKE_CLAW_RELEASE + OuttakeConstants.OUTTAKE_CLAW_CLOSED_TIME) / 1000.0)) {
                            stateMachine.changeState(RobotState.SAMPLE_OUTTAKE_DUMP);
                            follower.followPath(grabPickup2, true);
                            setPathState(5);
                        }
                    }
                }
                break;
            case 5:
                /* This case checks the robot's position and will wait until the robot position is close (1 inch away) from the pickup1Pose's position */
                if(!follower.isBusy()) {
                    // Start turret movement and extend slides immediately
                    rotateTurret(BasketAutoConstant.SAMPLE2_TURRET_POS);
                    rotateClawWrist(BasketAutoConstant.SAMPLE2_WRIST_POS);
                    robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MAX);
                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_PREP);
                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_PREP);


                    if(pathTimerElapsed(BasketAutoConstant.INTAKE_SLIDE_WAIT_TIME / 1000.0)) {
                        stateMachine.changeState(RobotState.SAMPLE_INTAKE_GRAB);
                        if (stateMachine.getCurrentState() == RobotState.SAMPLE_INTAKE_CHECK_SAMPLE){
                            stateMachine.changeState(RobotState.SAMPLE_INTAKE_WRIST_MOVE);
                            setPathState(9);
                        }
                    }
                }
                break;
            case 6:
                /* Wait for intake sequence to complete, then move to score */
                if(stateMachine.getCurrentState() == RobotState.COMPLETE_INTAKE) {
                    /* Intake complete, now go to score first sample */
                    robot.outtake.setOuttakeSlidePosition(OuttakeConstants.OUTTAKE_SLIDE_BASKET);
                    stateMachine.changeState(RobotState.SAMPLE_OUTTAKE_SLIDES_EXTEND);

                    follower.followPath(scorePickup2,true);
                    setPathState(7);
                }
                break;
            case 7:
                /* This case checks the robot's position and will wait until the robot position is close (1 inch away) from the scorePose's position */
                if(stateMachine != null && stateMachine.getCurrentState() == RobotState.SAMPLE_OUTTAKE_ELBOW_BASKET) {
                    /* Wait for claw release delay, then open claw */
                    if(pathTimerElapsed(BasketAutoConstant.OUTTAKE_CLAW_RELEASE / 1000.0)) {
                        robot.outtake.setOuttakeClawPosition(OuttakeConstants.CLAW_OPEN);
                        if(pathTimerElapsed((BasketAutoConstant.OUTTAKE_CLAW_RELEASE + OuttakeConstants.OUTTAKE_CLAW_CLOSED_TIME) / 1000.0)) {
                            stateMachine.changeState(RobotState.SAMPLE_OUTTAKE_DUMP);
                            follower.followPath(grabPickup3, true);
                            setPathState(8);
                        }
                    }
                }
                break;
            case 8:
                /* This case checks the robot's position and will wait until the robot position is close (1 inch away) from the pickup1Pose's position */
                if(!follower.isBusy()) {
                    // Start turret movement and extend slides immediately
                    rotateTurret(BasketAutoConstant.SAMPLE3_TURRET_POS);
                    rotateClawWrist(BasketAutoConstant.SAMPLE3_WRIST_POS);

                    robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MAX);
                    robot.intake.setIntakeShoulderPosition(IntakeConstants.SHOULDER_PREP);
                    robot.intake.setIntakeElbowPosition(IntakeConstants.ELBOW_PREP);


                    if(pathTimerElapsed(BasketAutoConstant.INTAKE_SLIDE_WAIT_TIME / 1000.0)) {
                        stateMachine.changeState(RobotState.SAMPLE_INTAKE_GRAB);
                        if (stateMachine.getCurrentState() == RobotState.SAMPLE_INTAKE_CHECK_SAMPLE){
                            stateMachine.changeState(RobotState.SAMPLE_INTAKE_WRIST_MOVE);
                            setPathState(9);
                        }
                    }
                }
                break;
            case 9:
                /* Wait for intake sequence to complete, then move to score third sample */
                if(stateMachine.getCurrentState() == RobotState.COMPLETE_INTAKE) {
                    /* Intake complete, now go to score third sample */
                    robot.outtake.setOuttakeSlidePosition(OuttakeConstants.OUTTAKE_SLIDE_BASKET);
                    stateMachine.changeState(RobotState.SAMPLE_OUTTAKE_SLIDES_EXTEND);
                    follower.followPath(scorePickup3,true);
                    setPathState(10);
                }
                break;
            case 10:
                /* Final scoring - wait for outtake sequence to complete, then end */
                if(stateMachine != null && stateMachine.getCurrentState() == RobotState.SAMPLE_OUTTAKE_ELBOW_BASKET) {
                    /* Wait for claw release delay, then open claw */
                    if(pathTimerElapsed(BasketAutoConstant.OUTTAKE_CLAW_RELEASE / 1000.0)) {
                        robot.outtake.setOuttakeClawPosition(OuttakeConstants.CLAW_OPEN);

                        /* Wait additional time for claw to open, then return to start */
                        if(pathTimerElapsed((BasketAutoConstant.OUTTAKE_CLAW_RELEASE + OuttakeConstants.OUTTAKE_CLAW_CLOSED_TIME) / 1000.0)) {
                            stateMachine.changeState(RobotState.SAMPLE_OUTTAKE_DUMP);

                            /* Retract outtake slides before returning to start */
                            robot.outtake.setOuttakeSlidePosition(OuttakeConstants.OUTTAKE_SLIDE_MIN);

                            /* Start return to start path */
                            follower.followPath(returnToStart, true);
                            setPathState(11);
                        }
                    }
                }
                break;
            case 11:
                /* Return to starting position */
                if(!follower.isBusy()) {
                    /* Robot has returned to start position - end autonomous */
                    telemetry.addData("Status", "Returned to start position");
                    setPathState(-1);
                }
                break;

            }
        } catch (Exception e) {
            // Safety catch to prevent crashes during path updates
            // DO NOT call setPathState(-1) to avoid recursive crashes
            // Just continue - the main loop will handle error display
        }
    }

    /** These change the states of the paths and actions
     * It will also reset the timers of the individual switches **/
    public void setPathState(int pState) {
        pathState = pState;
        pathTimer.resetTimer();
    }

    /**
     * Crash-safe wait using pathTimer (bulletproof approach)
     * This is the ONLY timing method you should use
     */
    public boolean pathTimerElapsed(double waitTimeSeconds) {
        try {
            return pathTimer != null && pathTimer.getElapsedTimeSeconds() >= waitTimeSeconds;
        } catch (Exception e) {
            // If timer fails, consider time elapsed to prevent hanging
            return true;
        }
    }




    // ===== AUTONOMOUS ACTION METHODS =====

    /** Extend intake slides to maximum extension */
    public void  extendIntakeSlides() {
        try {
            if(robot != null && robot.intake != null) {
                robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MAX);
                telemetry.addData("Action", "Extending intake slides");
            }
        } catch (Exception e) {
            telemetry.addData("Action Error", "Failed to extend intake slides");
        }
    }

    /** Retract intake slides to minimum position */
    public void retractIntakeSlides() {
        try {
            if(robot != null && robot.intake != null) {
                robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MIN);
                telemetry.addData("Action", "Retracting intake slides");
            }
        } catch (Exception e) {
            telemetry.addData("Action Error", "Failed to retract intake slides");
        }
    }

    /** Rotate turret to specified angle (in degrees) */
    public void rotateTurret(double servoPosition) {
        try {
            if(robot != null && robot.intake != null) {
                robot.intake.setIntakeTurretPosition(servoPosition);
            }
        } catch (Exception e) {
            // Silently fail - turret rotation is not critical
        }
    }

    public void rotateClawWrist(double servoPosition) {
        try {
            if(robot != null && robot.intake != null) {
                robot.intake.setIntakeWristPosition(servoPosition);
            }
        } catch (Exception e) {
            // Silently fail - wrist rotation is not critical
        }
    }

    /** Execute intake sequence using state machine */
    public void executeIntakeSequence() {
        try {
            if(stateMachine != null) {
                stateMachine.changeState(RobotState.SAMPLE_INTAKE_GRAB);
            }
        } catch (Exception e) {
            telemetry.addData("State Error", "Failed to start intake sequence");
        }
    }





    /** This is the main loop of the OpMode, it will run repeatedly after clicking "Play". **/
    @Override
    public void loop() {
        try {
            // If in stopped state, only update basic telemetry and return early
            if(pathState == -1) {
                // Use safe telemetry calls that won't crash
                telemetry.addData("Auto Status", "STOPPED - Testing Mode");
                telemetry.addData("Path State", pathState);
                // Safe timer access with null check
                if(pathTimer != null) {
                    try {
                        telemetry.addData("Path Timer", "%.2fs", pathTimer.getElapsedTimeSeconds());
                    } catch (Exception timerEx) {
                        telemetry.addData("Path Timer", "ERROR");
                    }
                }
                telemetry.update();
                return; // Exit early to avoid hardware updates
            }

            // Update robot systems (only when not stopped)
            if(follower != null) follower.update();
            if(buttonDetector != null) {
                try {
                    buttonDetector.update(new Gamepad());
                } catch (Exception btnEx) {
                    // Button detector failed, continue without it
                }
            }
            if(stateMachine != null) stateMachine.update();
            autonomousPathUpdate();

            // Basic telemetry for autonomous (with null checks)
            telemetry.addData("Path State", pathState);
            if(follower != null) {
                try {
                    telemetry.addData("Position", "X: %.1f, Y: %.1f",
                        follower.getPose().getX(), follower.getPose().getY());
                    telemetry.addData("Heading", "%.1f°", Math.toDegrees(follower.getPose().getHeading()));
                    telemetry.addData("Follower Busy", follower.isBusy());
                } catch (Exception followerEx) {
                    telemetry.addData("Position", "ERROR");
                }
            }
            if(stateMachine != null) {
                try {
                    telemetry.addData("State Machine", stateMachine.getCurrentState().toString());
                } catch (Exception stateEx) {
                    telemetry.addData("State Machine", "ERROR");
                }
            }
            telemetry.addData("Button Detector", buttonDetector != null ? "OK" : "NULL");
            telemetry.update();
        } catch (Exception e) {
            // CRITICAL: Prevent recursive crash loop by NOT calling setPathState(-1) here
            // Instead, just show error and continue
            try {
                telemetry.addData("CRITICAL ERROR", e.getClass().getSimpleName());
                telemetry.addData("Error Message", e.getMessage() != null ? e.getMessage() : "Unknown");
                telemetry.addData("Auto Status", "ERROR - Check logs");
                telemetry.update();
            } catch (Exception telemetryEx) {
                // If even telemetry fails, we can't do much - just continue
                // This prevents infinite crash loops
            }
        }
    }

    /** This method is called once at the init of the OpMode. **/
    @Override
    public void init() {
        pathTimer = new Timer();
        opmodeTimer = new Timer();
        opmodeTimer.resetTimer();

        // Initialize the robot hardware
        robot.init();
        robot.outtake.setOuttakeClawPosition(OuttakeConstants.CLAW_CLOSED);

        // Initialize the state machine with a dummy gamepad and sample mode for autonomous
        Gamepad dummyGamepad = new Gamepad();
        stateMachine = new RobotStateMachine(robot, telemetry, dummyGamepad, RobotMode.SAMPLE);
        stateMachine.setTeamColor(teamColor);
        stateMachine.setButtonDetector(buttonDetector); // Required for state machine validation

        // Initialize the follower with starting pose
        Constants.setConstants(FConstants.class, LConstants.class);
        follower = new Follower(hardwareMap, FConstants.class, LConstants.class);
        follower.setStartingPose(startPose);

        buildPaths();

        telemetry.addData("Status", "Hardware Initialized");
        telemetry.addData("Team Color", teamColor.displayName);
        telemetry.update();
    }

    /** This method is called continuously after Init while waiting for "play". **/
    @Override
    public void init_loop() {}

    /** This method is called once at the start of the OpMode.
     * It runs all the setup actions, including building paths and starting the path system **/
    @Override
    public void start() {
        opmodeTimer.resetTimer();
        setPathState(0);
    }

    /** We do not use this because everything should automatically disable **/
    @Override
    public void stop() {
    }
}