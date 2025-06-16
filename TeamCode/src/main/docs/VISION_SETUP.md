# FatDragon Vision System Setup

This is the **only** vision system in your codebase. The old enhanced vision system has been removed and replaced with the proven FatDragon-based implementation.

## Overview

Your vision system now uses **only** the FatDragon-based algorithms from FTC team 12527, which have been proven to work reliably in competition conditions.

## Files in Your System

### Python Files
- **`fatdragon_sample_detection.py`** - Main detection script (upload to Limelight Pipeline 1)
- **`vision_config.py`** - Configuration parameters (tunable values)

### Java Files
- **`VisionSystem.java`** - Main interface class for robot code
- **`VisionConstants.java`** - Java constants (matches Python config)
- **`FatDragonVisionTest.java`** - Test OpMode

### Documentation
- **`FATDRAGON_VISION_SETUP.md`** - Detailed setup instructions
- **`VISION_SETUP.md`** - This file (quick reference)

## Quick Setup

### 1. Upload Python Script
1. Access Limelight web interface: `http://limelight.local:5801`
2. Go to "Python" tab
3. Create new pipeline (index 1) named "FatDragon Detection"
4. Copy contents of `fatdragon_sample_detection.py` and paste
5. Save the script

### 2. Configure Pipeline
- Set resolution to 320x240
- Enable "Python Script" processing
- Set exposure to manual mode (-5 to -10)
- Set gain to 0

### 3. Test Detection
Run the `FatDragonVisionTest` OpMode to verify detection works.

### 4. Use in Robot Code
```java
// Initialize vision system
VisionSystem vision = new VisionSystem(this);
vision.init();
vision.setColorRecognitionEnabled(true);  // Enables FatDragon pipeline

// In your loop
vision.update();
if (vision.isColorSampleDetected()) {
    SampleColor color = vision.getDetectedColor();
    double x = vision.getSampleX();
    double y = vision.getSampleY();
    int confidence = vision.getConfidence();
    int distance = vision.getSampleDistance();  // Distance in inches
    
    if (vision.isHighConfidenceDetection(70)) {
        // High confidence detection - safe to act
        // Use the sample data for robot navigation
    }
}
```

## Key Features

### Proven Parameters
- **HSV Ranges**: Tested in real competition conditions
- **Processing Resolution**: 256x144 for optimal performance
- **Edge Detection**: Advanced Sobel + morphological operations
- **Color Classification**: 70% minimum overlap for reliability

### Data Format
The system returns:
- **Sample count**: Number of samples detected
- **Position**: X,Y coordinates (0-256, 0-144)
- **Color**: RED, BLUE, YELLOW, or NONE
- **Confidence**: Percentage (0-100)
- **Distance**: Estimated distance in inches
- **Quadrant**: TOP, BOTTOM, or CENTER
- **Angle**: Sample orientation in degrees

### Performance
- **Fast**: Optimized for competition loop times
- **Reliable**: 70%+ confidence threshold
- **Robust**: Handles touching/overlapping samples
- **Proven**: Based on successful competition implementation

## Tuning

To adjust detection for your conditions, modify values in `vision_config.py`:

```python
# Make detection more sensitive to smaller samples
MIN_CONTOUR_AREA = 150

# Detect in darker conditions
MIN_BRIGHTNESS_THRESHOLD = 45

# Be less strict about color matching
MIN_COLOR_OVERLAP = 0.6

# Adjust blue detection for your lighting
HSV_BLUE_RANGE = ([85, 100, 30], [145, 255, 255])
```

After changes, re-upload the Python script to Limelight.

## Troubleshooting

### No Samples Detected
1. Check lighting conditions
2. Verify Python script uploaded to Pipeline 1
3. Test with `FatDragonVisionTest` OpMode
4. Adjust HSV ranges if needed

### Poor Color Classification
1. Check for reflections/shadows on samples
2. Adjust `MIN_COLOR_OVERLAP` value
3. Try different lighting presets in `vision_config.py`

### Low Performance
1. Verify 256x144 processing resolution
2. Check Limelight CPU usage in web interface
3. Monitor loop times with telemetry

## Support

- **Detailed Setup**: See `FATDRAGON_VISION_SETUP.md`
- **Parameter Reference**: See `vision_config.py` comments
- **Test OpMode**: Use `FatDragonVisionTest.java`

The FatDragon-based system should provide reliable sample detection for your robot!
