# Intake Slide Auto-Return Functionality

## Overview
The intake slide auto-return functionality automatically moves the intake slide from the HOLD position to the MIN position after a configurable timer or when a limit switch is triggered. This replaces manual `setIntakeSlidePosition(SLIDE_HOLD)` calls with automatic return behavior.

## Key Features
- **Automatic return**: When slide moves to HOLD position, automatically returns to MIN after delay
- **Uses existing system**: Integrates with the existing `scheduleIntakeSlideReset` system
- **Drop-in replacement**: Simply replace `setIntakeSlidePosition(SLIDE_HOLD)` calls
- **Limit switch detection**: Enhanced reset system checks both timer and limit switch
- **No code duplication**: Reuses existing state machine infrastructure

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

#### 2. `setIntakeSlidePositionWithAutoReturn(double position)`
```java
public void setIntakeSlidePositionWithAutoReturn(double position)
```
- Drop-in replacement for `setIntakeSlidePosition(SLIDE_HOLD)` calls
- Automatically schedules return to MIN position when moving to HOLD
- Uses existing `scheduleIntakeSlideReset` system

### RobotStateMachine.java

#### 3. `scheduleIntakeSlideAutoReturn(long delayMs)`
```java
public void scheduleIntakeSlideAutoReturn(long delayMs)
```
- Public wrapper for existing `scheduleIntakeSlideReset` system
- Allows external classes to trigger auto-return

### BaseTeleop25152.java

#### 4. `getStateMachine()`
```java
public RobotStateMachine getStateMachine()
```
- Provides access to state machine for external classes
- Enables auto-return functionality integration

## Enhanced Functionality

### State Machine Integration
The existing intake slide reset system has been enhanced to include limit switch checking:
- Checks both timer expiration AND limit switch status
- Faster return when limit switch is triggered
- Fallback to timer for safety

## Usage Examples

### 1. Basic Auto-Return (Only change needed)
```java
// Replace this:
robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_HOLD);

// With this:
robot.intake.setIntakeSlidePositionWithAutoReturn(IntakeConstants.SLIDE_HOLD);
```

### 2. Other positions remain unchanged
```java
// These calls don't change:
robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MAX);
robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_MIN);
robot.intake.setIntakeSlidePosition(IntakeConstants.SLIDE_RELEASE);
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

## Configuration

### Timing Adjustment
Modify the auto-return delay in `IntakeConstants.java`:
```java
public static int SLIDE_AUTO_RETURN_DELAY = 800; // Adjust as needed
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
