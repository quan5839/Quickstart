# Vision Pipeline Test TeleOp Usage Guide

## Overview

The `VisionPipelineTestTeleop` allows you to test and switch between your three Python pipeline scripts in real-time. It's designed to work with the native Limelight3A SDK and your camera constants.

## Fixed Issues

✅ **Removed LimelightHelpers dependency** - Now uses native Limelight3A SDK  
✅ **Fixed import errors** - Uses correct Limelight3A classes  
✅ **Simplified dependencies** - Removed PedroPathing Pose dependency  
✅ **Added error handling** - Graceful handling of vision data errors  
✅ **Camera constants integration** - Uses your configured camera setup  

## Controls

### Pipeline Switching
- **DPAD_UP**: Switch to Blue pipeline (Pipeline 0)
- **DPAD_LEFT**: Switch to Red pipeline (Pipeline 1)
- **DPAD_DOWN**: Switch to Yellow pipeline (Pipeline 2)

### Camera Controls
- **X Button**: Toggle LED (placeholder - limited SDK support)
- **Y Button**: Take snapshot (placeholder - limited SDK support)
- **A Button**: Print detailed vision data to telemetry
- **B Button**: Reset camera settings

## Display Information

### Real-Time Data
- **Current Pipeline**: Shows which color detection is active
- **Target Found**: Whether samples are detected
- **Color Information**: Sample color, area, angle, total count
- **Distance**: Calculated distance using your camera constants
- **Robot Position**: X, Y coordinates relative to robot center
- **Turn Angle**: Degrees robot needs to turn to face sample
- **Performance**: Loop time and averages

### Camera Status
- **LED Status**: Current LED state
- **Snapshot Count**: Number of snapshots taken
- **Camera Constants**: Your configured height, tilt, offsets

## Camera Constants Integration

The teleop uses your camera constants for accurate calculations:

```java
// Your current settings from camera_constants.py
CAMERA_HEIGHT = 13.54 inches
CAMERA_TILT_ANGLE = 15.0 degrees
CAMERA_OFFSET_Y = -5.91 inches (5.91 inches to the LEFT of robot center)
```

## Coordinate System

### Robot Position Output
- **Robot X**: Left/Right from robot center (+ = right, - = left)
- **Robot Y**: Forward/Backward from robot center (+ = forward, - = backward)
- **Turn Angle**: Degrees to turn to face sample

### Your Camera Setup
- **CAMERA_OFFSET_Y = -5.91** means camera is **5.91 inches to the LEFT** of robot center
- This is automatically accounted for in position calculations

## Python Pipeline Data

The teleop reads the enhanced `llpython` array from your Python scripts:

```python
llpython = [
    target_found,        # [0] 1 if sample detected, 0 otherwise
    x_coordinate,        # [1] X position of sample
    y_coordinate,        # [2] Y position of sample
    angle,              # [3] Orientation angle
    contour_count,      # [4] Number of contours for this color
    color_code,         # [5] Color code (1=Blue, 2=Red, 3=Yellow)
    total_samples,      # [6] Total samples detected
    area                # [7] Area of the sample
]
```

## Usage Instructions

### 1. Upload Python Scripts
Upload your three pipeline scripts to the Limelight:
- **Pipeline 0**: `limelightblue_llm_debug_latest.py`
- **Pipeline 1**: `limelightred_llm_debug_latest.py`
- **Pipeline 2**: `limelightyellow_llm_debug_latest.py`

### 2. Run the TeleOp
1. Select "Vision Pipeline Test" from Driver Station
2. Initialize and start the OpMode
3. Use DPAD to switch between pipelines
4. Observe real-time detection results

### 3. Test Each Pipeline
- **Blue Pipeline**: Test blue sample detection
- **Red Pipeline**: Test red sample detection (with dual HSV ranges)
- **Yellow Pipeline**: Test yellow sample detection

### 4. Validate Distance Accuracy
- Place samples at known distances (12", 24", 36", etc.)
- Compare calculated distance with actual distance
- Adjust camera constants if needed

### 5. Check Robot Positioning
- Verify X, Y coordinates make sense for sample positions
- Check turn angles for accuracy
- Validate camera offset calculations

## Troubleshooting

### No Vision Data
- Check Limelight connection and power
- Verify Python scripts are uploaded to correct pipelines
- Ensure camera constants are properly configured

### Inaccurate Distance
- Verify `CAMERA_HEIGHT` measurement
- Check `CAMERA_TILT_ANGLE` setting
- Validate sample height assumption (1.0 inch)

### Wrong Position Calculations
- Verify `CAMERA_OFFSET_X` and `CAMERA_OFFSET_Y` measurements
- Check coordinate system understanding
- Validate field of view constants

### Performance Issues
- Monitor loop times in telemetry
- Check for vision processing errors
- Verify Limelight pipeline performance

## Expected Output Example

```
=== VISION PIPELINE TEST ===
Current Pipeline: Blue (0)
Runtime: 45.2 sec
Loop Time: 12.3 ms (avg: 15.1 ms)

=== DETECTION RESULTS ===
Target Found: true
Color: Blue (code: 1)
Total Samples: 2
Sample Area: 1250
Sample Angle: 45.2°

=== POSITIONING ===
Distance: 24.5 in
Robot X: -8.3 in (left of center)
Robot Y: 22.1 in (forward of center)
Turn Angle: 15.2°

=== CAMERA STATUS ===
LED: ON
Snapshots: 3

=== CONTROLS ===
DPAD: Switch Pipeline | X: LED | Y: Snapshot
A: Print Data | B: Reset Camera
```

## Integration with Robot Code

Once validated, you can use the same vision data structure in your robot control code:

```java
// Get vision result
VisionResult result = visionSystem.getLatestResult();

// Use positioning data for path planning
if (result.targetFound) {
    double distanceToSample = result.distance;
    double robotX = result.robotPosition.x;
    double robotY = result.robotPosition.y;
    double turnAngle = result.robotPosition.heading;
    
    // Plan robot movement to sample
    planPathToSample(robotX, robotY, turnAngle);
}
```

This teleop provides everything you need to test, validate, and tune your three Python pipeline scripts with comprehensive real-time feedback!
