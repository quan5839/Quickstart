# Intake Slide Auto-Return Functionality

## Overview
The intake slide auto-return functionality automatically moves the intake slide from the HOLD position to the MIN position after a configurable timer or when a limit switch is triggered. This replaces manual slide positioning and ensures consistent behavior.

## Key Features
- **Timer-based auto-return**: Configurable delay before returning to minimum position
- **Limit switch detection**: Faster return when mechanical limit switch is triggered
- **Drop-in replacement**: Can replace existing `setIntakeSlidePosition` calls
- **State machine integration**: Works with the existing state machine reset system
- **Thread-safe**: Safe for use in autonomous and teleop modes

## New Constants Added

### IntakeConstants.java
```java
/** Auto-return delay from hold to min position (ms) */
public static int SLIDE_AUTO_RETURN_DELAY = 800;
```

## New Methods Added

### IntakeSystem.java

#### 1. `isSlideAtMinPosition()`
```java
public boolean isSlideAtMinPosition()
```
- Checks if slide has reached minimum position using limit switch or position feedback
- Similar to existing `isSlideAtHoldPosition()` method

#### 2. `setIntakeSlidePositionSmart(double position, boolean enableAutoReturn)`
```java
public void setIntakeSlidePositionSmart(double position, boolean enableAutoReturn)
```
- Smart positioning with optional auto-return functionality
- Integrates with state machine for optimal performance
- Use when you have access to the state machine (teleop mode)

#### 3. `setIntakeSlidePositionSmart(double position)`
```java
public void setIntakeSlidePositionSmart(double position)
```
- Smart positioning with auto-return enabled by default
- Convenience method for most use cases

#### 4. `setIntakeSlidePositionWithAutoReturn(double position)`
```java
public void setIntakeSlidePositionWithAutoReturn(double position)
```
- Thread-based auto-return for autonomous or when state machine is unavailable
- Drop-in replacement for `setIntakeSlidePosition`
- Works independently of state machine

### RobotStateMachine.java

#### 5. `scheduleIntakeSlideAutoReturn(long delayMs)`
```java
public void scheduleIntakeSlideAutoReturn(long delayMs)
```
- Public method to schedule auto-return from external classes
- Integrates with existing intake slide reset system

### BaseTeleop25152.java

#### 6. `getStateMachine()`
```java
public RobotStateMachine getStateMachine()
```
- Provides access to state machine for external classes
- Enables smart positioning integration

## Enhanced Functionality

### State Machine Integration
The existing intake slide reset system has been enhanced to include limit switch checking:
- Checks both timer expiration AND limit switch status
- Faster return when limit switch is triggered
- Fallback to timer for safety

## Usage Examples

### 1. Basic Auto-Return (Recommended)
```java
// Replace this:
robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_HOLD);

// With this:
robot.intake.setIntakeSlidePositionWithAutoReturn(IntakeConstants.SLIDE_HOLD);
```

### 2. State Machine Integration (Teleop)
```java
// For optimal performance in teleop:
robot.intake.setIntakeSlidePositionSmart(IntakeConstants.SLIDE_HOLD);
```

### 3. Conditional Auto-Return
```java
// Enable auto-return only in certain conditions:
boolean shouldAutoReturn = (currentState == RobotState.SAMPLE_INTAKE_CLOSE);
robot.intake.setIntakeSlidePositionSmart(IntakeConstants.SLIDE_HOLD, shouldAutoReturn);
```

### 4. Autonomous Usage
```java
// In autonomous, use the thread-based approach:
robot.intake.setIntakeSlidePositionWithAutoReturn(IntakeConstants.SLIDE_HOLD);
// Slide will automatically return to min position after SLIDE_AUTO_RETURN_DELAY
```

## Migration Guide

### Step 1: Identify Current Usage
Find all instances of:
```java
robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_HOLD);
```

### Step 2: Replace with Auto-Return
Replace with:
```java
robot.intake.setIntakeSlidePositionWithAutoReturn(IntakeConstants.SLIDE_HOLD);
```

### Step 3: Remove Manual Reset Code
Remove any manual slide reset code that moves from HOLD to MIN:
```java
// Remove this type of code:
if (slideAtHoldPosition) {
    robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MIN);
}
```

### Step 4: Update State Machine Transitions
For state machine transitions, consider using the smart method:
```java
robot.intake.setIntakeSlidePositionSmart(IntakeConstants.SLIDE_HOLD);
```

## Configuration

### Timing Adjustment
Modify the auto-return delay in `IntakeConstants.java`:
```java
public static int SLIDE_AUTO_RETURN_DELAY = 800; // Adjust as needed
```

### Disable Auto-Return
To disable auto-return for specific calls:
```java
robot.intake.setIntakeSlidePositionSmart(IntakeConstants.SLIDE_HOLD, false);
```

## Benefits

1. **Consistency**: Ensures slide always returns to safe position
2. **Performance**: Limit switch detection provides faster response
3. **Safety**: Timer fallback prevents stuck slides
4. **Simplicity**: Reduces code duplication and manual management
5. **Flexibility**: Multiple usage patterns for different scenarios

## Troubleshooting

### Issue: Slide doesn't auto-return
- Check that `SLIDE_AUTO_RETURN_DELAY` is set appropriately
- Verify limit switch is properly connected and configured
- Ensure you're calling the auto-return methods, not the basic `setIntakeSlidePosition`

### Issue: Auto-return too fast/slow
- Adjust `IntakeConstants.SLIDE_AUTO_RETURN_DELAY`
- Check limit switch sensitivity

### Issue: State machine integration not working
- Verify `getStateMachine()` returns a valid instance
- Check that you're in a teleop mode that extends `BaseTeleop25152`
