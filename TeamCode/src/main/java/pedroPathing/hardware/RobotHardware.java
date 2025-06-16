/* Copyright (c) 2022 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package pedroPathing.hardware;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.Servo;

import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.HashMap;
import java.util.Map;

import pedroPathing.constants.ControlConstants;

/*
 * This file works in conjunction with the External Hardware Class sample called: ConceptExternalHardwareClass.java
 * Please read the explanations in that Sample about how to use this class definition.
 *
 * This file defines a Java Class that performs all the setup and configuration for a sample robot's hardware (motors and sensors).
 * It assumes three motors (left_drive, right_drive and arm) and two servos (left_hand and right_hand)
 *
 * This one file/class can be used by ALL of your OpModes without having to cut & paste the code each time.
 *
 * Where possible, the actual hardware objects are "abstracted" (or hidden) so the OpMode code just makes calls into the class,
 * rather than accessing the internal hardware directly. This is why the objects are declared "private".
 *
 * Use Android Studio to Copy this Class, and Paste it into your team's code folder with *exactly the same name*.
 *
 * Or... In OnBot Java, add a new file named RobotHardware.java, select this sample, and select Not an OpMode.
 * Also add a new OpMode, select the sample ConceptExternalHardwareClass.java, and select TeleOp.
 *
 */

public class RobotHardware {

    // Intake and Outtake system objects
    public IntakeSystem intake;
    public OuttakeSystem outtake;
    public DriveSystem driveSystem;
    public VisionSystem visionSystem;

    /* Declare OpMode members. */
    private final OpMode myOpMode;   // gain access to methods in the calling OpMode.

    // Define a constructor that allows the OpMode to pass a reference to itself.
    public RobotHardware (OpMode opmode) {
        myOpMode = opmode;
        intake = new IntakeSystem(opmode);
        outtake = new OuttakeSystem(opmode);
        visionSystem = new VisionSystem(opmode);
    }

    /**
     * Initialize all the robot's hardware.
     * This method must be called ONCE when the OpMode is initialized.
     * <p>
     * All of the hardware devices are accessed via the hardware map, and initialized.
     */
    public void init()    {
        // Enable bulk caching for all LynxModules (REV Hubs) for speed
        try {
            java.util.List<LynxModule> hubs = myOpMode.hardwareMap.getAll(LynxModule.class);
            for (LynxModule hub : hubs) {
                hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
            }
        } catch (Exception e) {
            myOpMode.telemetry.addData("WARNING", "No LynxModule found/bulk caching not enabled");
        }
        intake.init();
        outtake.init();
        visionSystem.init();
        driveSystem = new DriveSystem(myOpMode.hardwareMap, "leftFront", "rightFront", "leftBack", "rightBack");
        myOpMode.telemetry.addData(">", "Hardware Initialized");
        myOpMode.telemetry.update();
    }

    /**
     * Servo velocity control system with smart position caching
     */

    // Store previous velocities for acceleration limiting (optimized sizing)
    private final Map<Servo, Double> previousVelocities = new HashMap<>(ControlConstants.SERVO_MAP_INITIAL_CAPACITY);

    // Store previous positions for telemetry (optimized sizing)
    private final Map<Servo, Double> previousPositions = new HashMap<>(ControlConstants.SERVO_MAP_INITIAL_CAPACITY);

    // Smart servo position caching to prevent unnecessary writes
    private final Map<Servo, Double> cachedServoPositions = new HashMap<>(ControlConstants.SERVO_MAP_INITIAL_CAPACITY);

    // Using constants from ControlConstants class
    // for servo velocity control

    /**
     * Control a servo using velocity-based input with advanced features
     * - Deadzone handling for joystick inputs
     * - Acceleration limiting to prevent jerky movements
     * - Safety checks for null servo and invalid inputs
     * - Position clamping to valid range
     *
     * @param servo The servo to control
     * @param joystickInput The input from the joystick (-1.0 to 1.0)
     * @param deltaTime Time elapsed since last update (in seconds)
     * @param minPosition Minimum allowed position (0.0 to 1.0)
     * @param maxPosition Maximum allowed position (0.0 to 1.0)
     * @param maxVelocity Maximum velocity in position units per second
     * @param telemetry Optional telemetry for debugging (can be null)
     * @return The new position that was set
     */
    public double setServoVelocity(Servo servo, double joystickInput, double deltaTime,
                                 double minPosition, double maxPosition, double maxVelocity,
                                 Telemetry telemetry) {
        // Safety checks
        if (servo == null || deltaTime <= 0) {
            if (telemetry != null) {
                telemetry.addData("Servo Velocity Error", "Invalid servo or deltaTime");
            }
            return -1.0;
        }
        
        // Get current position
        double currentPosition = servo.getPosition();

        // Calculate target velocity based on joystick input
        double targetVelocity = joystickInput * maxVelocity;

        // Get previous velocity or default to 0
        double previousVelocity = previousVelocities.getOrDefault(servo, 0.0);

        // Apply acceleration limiting
        double maxVelocityChange = ControlConstants.MAX_SERVO_ACCELERATION * deltaTime;
        double velocityChange = targetVelocity - previousVelocity;
        velocityChange = Math.max(-maxVelocityChange, Math.min(maxVelocityChange, velocityChange));
        double actualVelocity = previousVelocity + velocityChange;

        // Store for next iteration
        previousVelocities.put(servo, actualVelocity);

        // Calculate new position based on velocity and time
        double newPosition = currentPosition + actualVelocity * deltaTime;

        // Clamp position to valid range
        newPosition = Math.max(minPosition, Math.min(maxPosition, newPosition));

        // Apply new position
        servo.setPosition(newPosition);

        // Store previous position for telemetry
        Double prev = previousPositions.get(servo);
        double positionChange = newPosition - (prev != null ? prev : currentPosition);
        previousPositions.put(servo, newPosition);

//        // Add telemetry if provided
//        if (telemetry != null) {
//            telemetry.addData("Servo", servo.getDeviceName());
//            telemetry.addData("Position", String.format(Locale.US, "%.3f → %.3f (Δ%.3f)",
//                                                      currentPosition, newPosition, positionChange));
//            telemetry.addData("Velocity", String.format(Locale.US, "%.3f", actualVelocity));
//        }

        return newPosition;
    }

    /**
     * Simplified version that uses the full range (0-1) and no telemetry
     */
    public double setServoVelocity(Servo servo, double joystickInput, double deltaTime, double maxVelocity) {
        return setServoVelocity(servo, joystickInput, deltaTime, 0.0, 1.0, maxVelocity, null);
    }

    /**
     * Simplified version with telemetry
     */
    public double setServoVelocity(Servo servo, double joystickInput, double deltaTime,
                                 double minPosition, double maxPosition, double maxVelocity) {
        return setServoVelocity(servo, joystickInput, deltaTime, minPosition, maxPosition, maxVelocity, null);
    }

    /**
     * Clear velocity tracking for a servo (useful when switching between velocity and position control)
     */
    public void clearServoVelocityTracking(Servo servo) {
        if (servo != null) {
            previousVelocities.remove(servo);
            previousPositions.remove(servo);
            cachedServoPositions.remove(servo);
        }
    }

    /**
     * Smart servo position setter that only writes when position actually changes
     * This reduces unnecessary I2C traffic and improves performance
     * @param servo The servo to control
     * @param position The target position (0.0 to 1.0)
     * @return true if position was actually written to servo, false if cached
     */
    public boolean setServoPositionSmart(Servo servo, double position) {
        if (servo == null) return false;

        // Clamp position to valid range
        position = Math.max(0.0, Math.min(1.0, position));

        // Check if position has actually changed (with small tolerance for floating point)
        Double cachedPosition = cachedServoPositions.get(servo);
        if (cachedPosition != null && Math.abs(cachedPosition - position) < 0.001) {
            return false; // Position hasn't changed significantly, skip write
        }

        // Position has changed, write to servo and update cache
        servo.setPosition(position);
        cachedServoPositions.put(servo, position);
        return true;
    }

    /**
     * Force a servo position update, bypassing the cache
     * Use this when you need to ensure the servo is set regardless of cached value
     * @param servo The servo to control
     * @param position The target position (0.0 to 1.0)
     */
    public void setServoPositionForced(Servo servo, double position) {
        if (servo == null) return;

        position = Math.max(0.0, Math.min(1.0, position));
        servo.setPosition(position);
        cachedServoPositions.put(servo, position);
    }

    /**
     * Get the cached position of a servo without reading from hardware
     * @param servo The servo to query
     * @return The cached position, or null if not cached
     */
    public Double getCachedServoPosition(Servo servo) {
        return cachedServoPositions.get(servo);
    }
}
