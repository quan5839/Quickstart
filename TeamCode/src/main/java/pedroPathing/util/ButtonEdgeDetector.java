package pedroPathing.util;

import com.qualcomm.robotcore.hardware.Gamepad;

/**
 * Utility class for gamepad button edge detection.
 * Shared between teleop and state machine for consistent behavior.
 *
 * Usage: Call update() once per loop, then use the pressed() methods to check for button presses.
 * The update() method can be called at the beginning of the loop.
 */
public class ButtonEdgeDetector {
    private boolean prevDpadUp, prevDpadDown, prevDpadLeft, prevDpadRight;
    private boolean prevStart, prevX, prevA, prevB, prevY;
    private boolean prevLeftBumper, prevRightBumper, prevBack;

    // Current frame state - captured during update()
    private boolean currDpadUp, currDpadDown, currDpadLeft, currDpadRight;
    private boolean currStart, currX, currA, currB, currY;
    private boolean currLeftBumper, currRightBumper, currBack;

    /**
     * Update the edge detector with current gamepad state
     * Call this once per loop cycle, typically at the beginning
     */
    public void update(Gamepad gamepad) {
        if (gamepad == null) return;

        // Move current state to previous
        prevDpadUp = currDpadUp;
        prevDpadDown = currDpadDown;
        prevDpadLeft = currDpadLeft;
        prevDpadRight = currDpadRight;
        prevStart = currStart;
        prevX = currX;
        prevA = currA;
        prevB = currB;
        prevY = currY;
        prevLeftBumper = currLeftBumper;
        prevRightBumper = currRightBumper;
        prevBack = currBack;

        // Capture new current state
        currDpadUp = gamepad.dpad_up;
        currDpadDown = gamepad.dpad_down;
        currDpadLeft = gamepad.dpad_left;
        currDpadRight = gamepad.dpad_right;
        currStart = gamepad.start;
        currX = gamepad.x;
        currA = gamepad.a;
        currB = gamepad.b;
        currY = gamepad.y;
        currLeftBumper = gamepad.left_bumper;
        currRightBumper = gamepad.right_bumper;
        currBack = gamepad.back;
    }

    // Edge detection methods - return true only on the frame when button is first pressed
    // These use the captured state from update(), so gamepad parameter is kept for compatibility
    public boolean dpadUpPressed(Gamepad gamepad) {
        return currDpadUp && !prevDpadUp;
    }

    public boolean dpadDownPressed(Gamepad gamepad) {
        return currDpadDown && !prevDpadDown;
    }

    public boolean dpadLeftPressed(Gamepad gamepad) {
        return currDpadLeft && !prevDpadLeft;
    }

    public boolean dpadRightPressed(Gamepad gamepad) {
        return currDpadRight && !prevDpadRight;
    }

    public boolean startPressed(Gamepad gamepad) {
        return currStart && !prevStart;
    }

    public boolean xPressed(Gamepad gamepad) {
        return currX && !prevX;
    }

    public boolean yPressed(Gamepad gamepad) {
        return currY && !prevY;
    }

    public boolean aPressed(Gamepad gamepad) {
        return currA && !prevA;
    }

    public boolean bPressed(Gamepad gamepad) {
        return currB && !prevB;
    }

    public boolean leftBumperPressed(Gamepad gamepad) {
        return currLeftBumper && !prevLeftBumper;
    }

    public boolean rightBumperPressed(Gamepad gamepad) {
        return currRightBumper && !prevRightBumper;
    }

    public boolean backPressed(Gamepad gamepad) {
        return currBack && !prevBack;
    }
}
