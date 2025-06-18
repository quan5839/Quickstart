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

import pedroPathing.constants.FConstants;
import pedroPathing.constants.IntakeConstants;
import pedroPathing.constants.LConstants;
import pedroPathing.constants.OuttakeConstants;
import pedroPathing.constants.SpecimenAutoConstants;
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

@Autonomous(name = "Specimen Auto Safe")
public class SpecimenAutoSafe extends OpMode {

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
    private final Pose startPose = new Pose(SpecimenAutoConstants.START_X, SpecimenAutoConstants.START_Y, Math.toRadians(SpecimenAutoConstants.START_HEADING_DEG));

    /** Scoring Pose of our robot. It is facing the submersible at a -45 degree (315 degree) angle. */
    private final Pose scorePreLoadPose = new Pose(SpecimenAutoConstants.SCORE_X, SpecimenAutoConstants.SCORE_PRELOAD_Y, Math.toRadians(SpecimenAutoConstants.GRAB_SCORE_HEADING_DEG));

    private final Pose pushPickup1PrepPose = new Pose(SpecimenAutoConstants.PUSH_PICKUP1_PREP_X, SpecimenAutoConstants.PUSH_PICKUP1_PREP_Y, Math.toRadians(SpecimenAutoConstants.GRAB_SCORE_HEADING_DEG));

    private final Pose pushPickup1PrepControlPose = new Pose(SpecimenAutoConstants.PUSH_PICKUP1_PREP_CONTROL_X, SpecimenAutoConstants.PUSH_PICKUP1_PREP_CONTROL_Y, Math.toRadians(SpecimenAutoConstants.GRAB_SCORE_HEADING_DEG));

    /** Lowest (First) Sample from the Spike Mark */
    private final Pose pushPickup1Pose = new Pose(SpecimenAutoConstants.PUSH_PICKUP1_X, SpecimenAutoConstants.PUSH_PICKUP1_Y, Math.toRadians(SpecimenAutoConstants.GRAB_SCORE_HEADING_DEG));

    private final Pose pushPickup1ControlPose = new Pose(SpecimenAutoConstants.PUSH_PICKUP1_CONTROL_X, SpecimenAutoConstants.PUSH_PICKUP1_CONTROL_Y);

    private final Pose pushRetrieve1Pose = new Pose(SpecimenAutoConstants.PUSH_RETRIEVE_PICKUP_X, SpecimenAutoConstants.PUSH_RETRIEVE_PICKUP1_Y, Math.toRadians(SpecimenAutoConstants.GRAB_SCORE_HEADING_DEG));

    /** Middle (Second) Sample from the Spike Mark */
    private final Pose pushPickup2Pose = new Pose(SpecimenAutoConstants.PUSH_PICKUP2_X, SpecimenAutoConstants.PUSH_PICKUP2_Y, Math.toRadians(SpecimenAutoConstants.GRAB_SCORE_HEADING_DEG));

    private final Pose pushPickup2ControlPose = new Pose(SpecimenAutoConstants.PUSH_PICKUP2_CONTROL_X, SpecimenAutoConstants.PUSH_PICKUP2_CONTROL_Y);

    private final Pose retrieve2Pose = new Pose(SpecimenAutoConstants.PUSH_RETRIEVE_PICKUP_X, SpecimenAutoConstants.PUSH_RETRIEVE_PICKUP2_Y, Math.toRadians(SpecimenAutoConstants.GRAB_SCORE_HEADING_DEG));


    /** Highest (Third) Sample from the Spike Mark */
    private final Pose pushPickup3Pose = new Pose(SpecimenAutoConstants.PUSH_PICKUP3_X, SpecimenAutoConstants.PUSH_PICKUP3_Y, Math.toRadians(SpecimenAutoConstants.GRAB_SCORE_HEADING_DEG));

    private final Pose pushPickup3ControlPose = new Pose(SpecimenAutoConstants.PUSH_PICKUP3_CONTROL_X, SpecimenAutoConstants.PUSH_PICKUP3_CONTROL_Y);

    private final Pose retrieve3Pose = new Pose(SpecimenAutoConstants.PUSH_RETRIEVE_PICKUP_X, SpecimenAutoConstants.PUSH_RETRIEVE_PICKUP3_Y, Math.toRadians(SpecimenAutoConstants.GRAB_SCORE_HEADING_DEG));

    private final Pose prepGrabSpecimenPose = new Pose(SpecimenAutoConstants.PREP_GRAB_SPECIMEN_X, SpecimenAutoConstants.PREP_AND_GRAB_SPECIMEN_Y, Math.toRadians(SpecimenAutoConstants.GRAB_SCORE_HEADING_DEG));
    private final Pose prepGrabSpecimenPoseControl1 = new Pose(SpecimenAutoConstants.PREP_GRAB_SPECIMEN_CONTROL_X_1, SpecimenAutoConstants.PREP_GRAB_SPECIMEN_CONTROL_Y_1);
    private final Pose prepGrabSpecimenPoseControl2 = new Pose(SpecimenAutoConstants.PREP_GRAB_SPECIMEN_CONTROL_X_2, SpecimenAutoConstants.PREP_GRAB_SPECIMEN_CONTROL_Y_2);

    private final Pose grabSpecimenPose = new Pose(SpecimenAutoConstants.GRAB_SPECIMEN_X, SpecimenAutoConstants.PREP_AND_GRAB_SPECIMEN_Y, Math.toRadians(SpecimenAutoConstants.GRAB_SCORE_HEADING_DEG));

    private final Pose scoreSpecimen1Pose = new Pose(SpecimenAutoConstants.SCORE_X, SpecimenAutoConstants.SCORE1_Y, Math.toRadians(SpecimenAutoConstants.GRAB_SCORE_HEADING_DEG));
    private final Pose scoreSpecimen2Pose = new Pose(SpecimenAutoConstants.SCORE_X, SpecimenAutoConstants.SCORE2_Y, Math.toRadians(SpecimenAutoConstants.GRAB_SCORE_HEADING_DEG));
    private final Pose scoreSpecimen3Pose = new Pose(SpecimenAutoConstants.SCORE_X, SpecimenAutoConstants.SCORE3_Y, Math.toRadians(SpecimenAutoConstants.GRAB_SCORE_HEADING_DEG));
    private final Pose scoreSpecimenPose = new Pose(SpecimenAutoConstants.SCORE_X, SpecimenAutoConstants.SCORE4_Y, Math.toRadians(SpecimenAutoConstants.GRAB_SCORE_HEADING_DEG));

    private final Pose scoreSpecimenControlPose = new Pose(SpecimenAutoConstants.SCORE_CONTROL_X, SpecimenAutoConstants.SCORE_CONTROL_Y);

    /** Park Pose for our robot, after we do all of the scoring. */
    private final Pose parkPose = new Pose(SpecimenAutoConstants.PARK_X, SpecimenAutoConstants.PARK_Y, Math.toRadians(SpecimenAutoConstants.PARK_HEADING_DEG));

    /** Park Control Pose for our robot, this is used to manipulate the bezier curve that we will create for the parking.
     * The Robot will not go to this pose, it is used a control point for our bezier curve. */
    private final Pose parkControlPose = new Pose(SpecimenAutoConstants.PARK_CONTROL_X, SpecimenAutoConstants.PARK_CONTROL_Y);

    /* These are our Paths and PathChains that we will define in buildPaths() */
    private Path scorePreload, park, returnToStart;
    private PathChain pushPickup1Prep, pushPickup1, pushPickup2, pushPickup3,
            retrievePickup1, retrievePickup2, retrievePickup3,
            prepGrabSpecimen1, prepGrabSpecimen2, prepGrabSpecimen3, prepGrabSpecimen4,
            scoreSpecimen1, scoreSpecimen2, scoreSpecimen3, scoreSpecimen4,
            grabSpecimen;

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
        scorePreload = new Path(new BezierLine(new Point(startPose), new Point(scorePreLoadPose)));
        scorePreload.setLinearHeadingInterpolation(startPose.getHeading(), scorePreLoadPose.getHeading());

        // Push pickup 1 sequence: prep -> push -> retrieve
        Path pushPickup1PrepPath = new Path(new BezierCurve(new Point(scorePreLoadPose), new Point(pushPickup1PrepControlPose), new Point(pushPickup1PrepPose)));
        pushPickup1PrepPath.setLinearHeadingInterpolation(scorePreLoadPose.getHeading(), pushPickup1PrepPose.getHeading());
        pushPickup1Prep = new PathChain(pushPickup1PrepPath);

        Path pushPickup1Path = new Path(new BezierCurve(new Point(pushPickup1PrepPose), new Point(pushPickup1ControlPose), new Point(pushPickup1Pose)));
        pushPickup1Path.setLinearHeadingInterpolation(pushPickup1PrepPose.getHeading(), pushPickup1Pose.getHeading());
        pushPickup1 = new PathChain(pushPickup1Path);

        Path retrivePickup1Path = new Path(new BezierLine(new Point(pushPickup1Pose), new Point(pushRetrieve1Pose)));
        retrivePickup1Path.setLinearHeadingInterpolation(pushPickup1Pose.getHeading(), pushRetrieve1Pose.getHeading());
        retrievePickup1 = new PathChain(retrivePickup1Path);



        Path pushPickup2Path = new Path(new BezierCurve(new Point(pushRetrieve1Pose), new Point(pushPickup2ControlPose), new Point(pushPickup2Pose)));
        pushPickup2Path.setLinearHeadingInterpolation(pushRetrieve1Pose.getHeading(), pushPickup2Pose.getHeading());
        pushPickup2 = new PathChain(pushPickup2Path);

        Path retrivePickup2Path = new Path(new BezierLine(new Point(pushPickup2Pose), new Point(retrieve2Pose)));
        retrivePickup2Path.setLinearHeadingInterpolation(pushPickup2Pose.getHeading(), retrieve2Pose.getHeading());
        retrievePickup2 = new PathChain(retrivePickup2Path);



        Path pushPickup3Path = new Path(new BezierCurve(new Point(retrieve2Pose), new Point(pushPickup3ControlPose), new Point(pushPickup3Pose)));
        pushPickup3Path.setLinearHeadingInterpolation(retrieve2Pose.getHeading(), pushPickup3Pose.getHeading());
        pushPickup3 = new PathChain(pushPickup3Path);

        Path retrivePickup3Path = new Path(new BezierLine(new Point(pushPickup3Pose), new Point(retrieve3Pose)));
        retrivePickup3Path.setLinearHeadingInterpolation(pushPickup3Pose.getHeading(), retrieve3Pose.getHeading());
        retrievePickup3 = new PathChain(retrivePickup3Path);



        Path prepGrabSpecimen1Path = new Path(new BezierCurve(new Point(retrieve3Pose), new Point(prepGrabSpecimenPoseControl1), new Point(prepGrabSpecimenPoseControl2), new Point(prepGrabSpecimenPose)));
        prepGrabSpecimen1Path.setLinearHeadingInterpolation(retrieve3Pose.getHeading(), prepGrabSpecimenPose.getHeading());
        prepGrabSpecimen1 = new PathChain(prepGrabSpecimen1Path);

        Path scoreSpecimen1Path = new Path(new BezierCurve(new Point(grabSpecimenPose), new Point(scoreSpecimenControlPose), new Point(scoreSpecimen1Pose)));
        scoreSpecimen1Path.setLinearHeadingInterpolation(grabSpecimenPose.getHeading(), scoreSpecimen1Pose.getHeading());
        scoreSpecimen1 = new PathChain(scoreSpecimen1Path);



        Path prepGrabSpecimen2Path = new Path(new BezierCurve(new Point(scoreSpecimen1Pose), new Point(prepGrabSpecimenPoseControl1), new Point(prepGrabSpecimenPoseControl2), new Point(prepGrabSpecimenPose)));
        prepGrabSpecimen2Path.setLinearHeadingInterpolation(scoreSpecimen1Pose.getHeading(), prepGrabSpecimenPose.getHeading());
        prepGrabSpecimen2 = new PathChain(prepGrabSpecimen2Path);

        Path scoreSpecimen2Path = new Path(new BezierCurve(new Point(grabSpecimenPose), new Point(scoreSpecimenControlPose), new Point(scoreSpecimen2Pose)));
        scoreSpecimen2Path.setLinearHeadingInterpolation(grabSpecimenPose.getHeading(), scoreSpecimen2Pose.getHeading());
        scoreSpecimen2 = new PathChain(scoreSpecimen2Path);



        Path prepGrabSpecimen3Path = new Path(new BezierCurve(new Point(scoreSpecimen2Pose), new Point(prepGrabSpecimenPoseControl1), new Point(prepGrabSpecimenPoseControl2), new Point(prepGrabSpecimenPose)));
        prepGrabSpecimen3Path.setLinearHeadingInterpolation(scoreSpecimen2Pose.getHeading(), prepGrabSpecimenPose.getHeading());
        prepGrabSpecimen3 = new PathChain(prepGrabSpecimen3Path);

        Path scoreSpecimen3Path = new Path(new BezierCurve(new Point(grabSpecimenPose), new Point(scoreSpecimenControlPose), new Point(scoreSpecimen3Pose)));
        scoreSpecimen3Path.setLinearHeadingInterpolation(grabSpecimenPose.getHeading(), scoreSpecimen3Pose.getHeading());
        scoreSpecimen3 = new PathChain(scoreSpecimen3Path);



        Path prepGrabSpecimen4Path = new Path(new BezierCurve(new Point(scoreSpecimen3Pose), new Point(prepGrabSpecimenPoseControl1), new Point(prepGrabSpecimenPoseControl2), new Point(prepGrabSpecimenPose)));
        prepGrabSpecimen4Path.setLinearHeadingInterpolation(scoreSpecimen3Pose.getHeading(), prepGrabSpecimenPose.getHeading());
        prepGrabSpecimen4 = new PathChain(prepGrabSpecimen4Path);

        Path scoreSpecimen4Path = new Path(new BezierCurve(new Point(grabSpecimenPose), new Point(scoreSpecimenControlPose), new Point(scoreSpecimenPose)));
        scoreSpecimen4Path.setLinearHeadingInterpolation(grabSpecimenPose.getHeading(), scoreSpecimenPose.getHeading());
        scoreSpecimen4 = new PathChain(scoreSpecimen4Path);



        Path grabSpecimenPath = new Path(new BezierLine(new Point(prepGrabSpecimenPose), new Point(grabSpecimenPose)));
        grabSpecimenPath.setLinearHeadingInterpolation(prepGrabSpecimenPose.getHeading(), grabSpecimenPose.getHeading());
        grabSpecimen = new PathChain(grabSpecimenPath);

        park = new Path(new BezierCurve(new Point(scoreSpecimenPose),  new Point(parkControlPose), new Point(parkPose)));
        park.setLinearHeadingInterpolation(scorePreLoadPose.getHeading(), parkPose.getHeading());

        returnToStart = new Path(new BezierLine(new Point(scorePreLoadPose), new Point(startPose)));
        returnToStart.setLinearHeadingInterpolation(scorePreLoadPose.getHeading(), startPose.getHeading());
    }

    /** This switch is called continuously and runs the pathing, at certain points, it triggers the action state.
     * Everytime the switch changes case, it will reset the timer. (This is because of the setPathState() method)
     * The followPath() function sets the follower to run the specific path, but does NOT wait for it to finish before moving on. */
    public void autonomousPathUpdate() {
        // If already in error state, don't execute any path logic
        if(pathState == -1) {
            return;
        }

        switch (pathState) {
        case 0:
            robot.outtake.setSlidePosition(OuttakeConstants.SLIDE_LIFT);
            robot.outtake.setElbowPosition(OuttakeConstants.ELBOW_SCORE_SPECIMEN);
            robot.outtake.setWristPosition(OuttakeConstants.WRIST_SCORE_SPECIMEN);
            follower.followPath(scorePreload);
            setPathState(1);
            break;

        case 1:
            if(!follower.isBusy()) {
                robot.outtake.setSlidePosition(OuttakeConstants.SLIDE_SCORE);
                stateMachine.changeState(RobotState.SPECIMEN_OUTTAKE_RELEASE);
                setPathState(2);
            }
            break;
        case 2:
            if(stateMachine.getCurrentState() == RobotState.INIT) {
                stateMachine.changeState(RobotState.HOLD);
                follower.followPath(pushPickup1Prep, true);
                setPathState(3);
            }

            break;
        case 3:
            if(!follower.isBusy()) {
                // Move to push position for sample 1
                follower.followPath(pushPickup1, true);
                setPathState(4);
            }
            break;
        case 4:
            if (!follower.isBusy()){
                // Push sample 1 into pickup area - no intake operations needed
                follower.followPath(retrievePickup1, true);
                setPathState(5);
            }
            break;
        case 5:
            if (!follower.isBusy()){
                // Move to push sample 2
                follower.followPath(pushPickup2);
                setPathState(6);
            }
            break;

        case 6:
            if(!follower.isBusy()) {
                // Push sample 2 into pickup area - no intake operations needed
                follower.followPath(retrievePickup2, true);
                setPathState(7);
            }
            break;
        case 7:
            if (!follower.isBusy()){
                // Move to push sample 3
                follower.followPath(pushPickup3);
                setPathState(8);
            }
            break;
        case 8:
            if(!follower.isBusy()) {
                // Push sample 3 into pickup area - no intake operations needed
                follower.followPath(retrievePickup3, true);
                setPathState(9);
            }
            break;
        case 9:
            if (!follower.isBusy()){
                // All samples pushed, now prepare to grab first specimen
                stateMachine.changeState(RobotState.INIT);
                follower.followPath(prepGrabSpecimen1);
                setPathState(10);
            }
            break;

        case 10:
            if (!follower.isBusy()){
                follower.followPath(grabSpecimen);
                setPathState(11);
            }
            break;

        case 11:
            if (!follower.isBusy()){
                robot.outtake.setClawPosition(OuttakeConstants.CLAW_CLOSED);
                if (stateMachine.getCurrentState() == RobotState.SPECIMEN_OUTTAKE_CHECK) {
                    follower.followPath(scoreSpecimen1);
                    setPathState(12);
                }
            }
            break;
        case 12:
            if(!follower.isBusy()) {
                robot.outtake.setSlidePosition(OuttakeConstants.SLIDE_SCORE);
                stateMachine.changeState(RobotState.SPECIMEN_OUTTAKE_RELEASE);
                setPathState(13);
            }
            break;
        case 13:
                if(stateMachine.getCurrentState() == RobotState.INIT) {
                    follower.followPath(prepGrabSpecimen2, true);
                    setPathState(14);
                }
            break;
        case 14:
                if(!follower.isBusy()) {
                    follower.followPath(grabSpecimen, true);
                    setPathState(15);
                }
            break;

        case 15:
            if (!follower.isBusy()){
                robot.outtake.setClawPosition(OuttakeConstants.CLAW_CLOSED);
                if (stateMachine.getCurrentState() == RobotState.SPECIMEN_OUTTAKE_CHECK) {
                    follower.followPath(scoreSpecimen2);
                    setPathState(16);
                }
            }
            break;
        case 16:
            if(!follower.isBusy()) {
                robot.outtake.setSlidePosition(OuttakeConstants.SLIDE_SCORE);
                stateMachine.changeState(RobotState.SPECIMEN_OUTTAKE_RELEASE);
                setPathState(17);
            }
            break;
        case 17:
            if(stateMachine.getCurrentState() == RobotState.INIT) {
                follower.followPath(prepGrabSpecimen3, true);
                setPathState(18);
            }
            break;

        case 18:
            if(!follower.isBusy()) {
                follower.followPath(grabSpecimen, true);
                setPathState(19);
            }
            break;

        case 19:
            if (!follower.isBusy()){
                robot.outtake.setClawPosition(OuttakeConstants.CLAW_CLOSED);
                if (stateMachine.getCurrentState() == RobotState.SPECIMEN_OUTTAKE_CHECK) {
                    follower.followPath(scoreSpecimen3);
                    setPathState(20);
                }
            }
            break;
        case 20:
            if(!follower.isBusy()) {
                robot.outtake.setSlidePosition(OuttakeConstants.SLIDE_SCORE);
                stateMachine.changeState(RobotState.SPECIMEN_OUTTAKE_RELEASE);
                setPathState(21);
            }
            break;
        case 21:
            if(stateMachine.getCurrentState() == RobotState.INIT) {
                follower.followPath(prepGrabSpecimen4, true);
                setPathState(22);
            }
            break;

        case 22:
            if(!follower.isBusy()) {
                follower.followPath(grabSpecimen, true);
                setPathState(23);
            }
            break;

        case 23:
            if (!follower.isBusy()){
                robot.outtake.setClawPosition(OuttakeConstants.CLAW_CLOSED);
                if (stateMachine.getCurrentState() == RobotState.SPECIMEN_OUTTAKE_CHECK) {
                    follower.followPath(scoreSpecimen4);
                    setPathState(24);
                }
            }
            break;
        case 24:
            if(!follower.isBusy()) {
                robot.outtake.setSlidePosition(OuttakeConstants.SLIDE_SCORE);
                stateMachine.changeState(RobotState.SPECIMEN_OUTTAKE_RELEASE);
                setPathState(25);
            }
            break;

        case 25:
            if(stateMachine.getCurrentState() == RobotState.INIT) {
                follower.followPath(park);
                setPathState(26);
            }
            break;

        case 26:
            if (!follower.isBusy()){
                robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MAX);
                setPathState(-1);
            }
            break;
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

    public void executeIntakeSampleRetrieve() {
        try {
            if(stateMachine != null) {
                stateMachine.changeState(RobotState.SAMPLE_INTAKE_WRIST_MOVE);
            }
        } catch (Exception e) {
            telemetry.addData("State Error", "Failed to start intake sequence");
        }    }

    /** Execute outtake sequence using state machine */
    public void executeOuttakeSequence() {
        try {
            if(stateMachine != null) {
                stateMachine.changeState(RobotState.SAMPLE_OUTTAKE_SLIDES_EXTEND);
            }
        } catch (Exception e) {
            telemetry.addData("State Error", "Failed to start outtake sequence");
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
        robot.outtake.setClawPosition(OuttakeConstants.CLAW_CLOSED);

        // Initialize the state machine with a dummy gamepad and sample mode for autonomous
        Gamepad dummyGamepad = new Gamepad();
        stateMachine = new RobotStateMachine(robot, telemetry, dummyGamepad, RobotMode.SPECIMEN);
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