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
import java.util.Locale;
import java.util.Map;

import pedroPathing.constants.ControlConstants;

/**
 * Robot hardware abstraction layer
 */

public class RobotHardware {

    public IntakeSystem intake;
    public OuttakeSystem outtake;
    public DriveSystem driveSystem;
    public VisionSystem visionSystem;

    private final OpMode opMode;

    public RobotHardware(OpMode opMode) {
        this.opMode = opMode;
        intake = new IntakeSystem(opMode);
        outtake = new OuttakeSystem(opMode);
        visionSystem = new VisionSystem(opMode);
    }

    public void init() {
        enableBulkCaching();
        intake.init();
        outtake.init();
        visionSystem.init();
        driveSystem = new DriveSystem(opMode.hardwareMap, "leftFront", "rightFront", "leftBack", "rightBack");
        opMode.telemetry.addData(">", "Hardware Initialized");
        opMode.telemetry.update();
    }

    private void enableBulkCaching() {
        try {
            java.util.List<LynxModule> hubs = opMode.hardwareMap.getAll(LynxModule.class);
            for (LynxModule hub : hubs) {
                hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
            }
        } catch (Exception e) {
            opMode.telemetry.addData("WARNING", "No LynxModule found/bulk caching not enabled");
        }
    }

    private final Map<Servo, Double> velocityCache = new HashMap<>(ControlConstants.SERVO_MAP_INITIAL_CAPACITY);
    private final Map<Servo, Double> positionCache = new HashMap<>(ControlConstants.SERVO_MAP_INITIAL_CAPACITY);
    private final Map<Servo, Double> servoPositions = new HashMap<>(ControlConstants.SERVO_MAP_INITIAL_CAPACITY);

    public double setServoVelocity(Servo servo, double input, double deltaTime,
                                 double minPos, double maxPos, double maxVel, Telemetry telemetry) {
        if (servo == null || deltaTime <= 0) {
            if (telemetry != null) {
                telemetry.addData("Servo Error", "Invalid servo or deltaTime");
            }
            return -1.0;
        }

        double currentPos = servo.getPosition();
        double targetVel = input * maxVel;
        double prevVel = velocityCache.getOrDefault(servo, 0.0);

        double maxVelChange = ControlConstants.MAX_SERVO_ACCELERATION * deltaTime;
        double velChange = targetVel - prevVel;
        velChange = Math.max(-maxVelChange, Math.min(maxVelChange, velChange));
        double actualVel = prevVel + velChange;

        velocityCache.put(servo, actualVel);

        double newPos = currentPos + actualVel * deltaTime;
        newPos = Math.max(minPos, Math.min(maxPos, newPos));

        servo.setPosition(newPos);

        Double prev = positionCache.get(servo);
        double posChange = newPos - (prev != null ? prev : currentPos);
        positionCache.put(servo, newPos);

        // Add telemetry if provided
        if (telemetry != null) {
            telemetry.addData("Position", String.format(Locale.US, "%.3f",newPos));
        }

        return newPos;
    }

    public double setServoVelocity(Servo servo, double input, double deltaTime, double maxVel) {
        return setServoVelocity(servo, input, deltaTime, 0.0, 1.0, maxVel, null);
    }

    public double setServoVelocity(Servo servo, double input, double deltaTime,
                                 double minPos, double maxPos, double maxVel) {
        return setServoVelocity(servo, input, deltaTime, minPos, maxPos, maxVel, null);
    }

    public void clearServoTracking(Servo servo) {
        if (servo != null) {
            velocityCache.remove(servo);
            positionCache.remove(servo);
            servoPositions.remove(servo);
        }
    }

    public boolean setServoPositionSmart(Servo servo, double position) {
        if (servo == null) return false;

        position = Math.max(0.0, Math.min(1.0, position));
        Double cached = servoPositions.get(servo);
        if (cached != null && Math.abs(cached - position) < ControlConstants.SERVO_POSITION_TOLERANCE) {
            return false;
        }

        servo.setPosition(position);
        servoPositions.put(servo, position);
        return true;
    }

    public void setServoPositionForced(Servo servo, double position) {
        if (servo == null) return;
        position = Math.max(0.0, Math.min(1.0, position));
        servo.setPosition(position);
        servoPositions.put(servo, position);
    }

    public Double getCachedServoPosition(Servo servo) {
        return servoPositions.get(servo);
    }
}
