# FatDragon-Based Vision System Setup

This document explains how to set up and use the new FatDragon-based sample detection system for your Limelight 3A.

## Overview

The new vision system is based on the proven working algorithms from FTC team 12527 FatDragon. Their implementation has been successfully tested and works reliably for sample detection. This implementation includes:

- **Proven HSV color ranges** that work in real competition conditions
- **Advanced edge detection** using Sobel operators with morphological operations
- **Robust contour separation** for detecting touching/overlapping samples
- **Sophisticated color classification** with overlap analysis
- **Performance optimization** with tracking and monitoring

## Key Improvements Over Previous System

1. **Better HSV Ranges**: Uses FatDragon's proven color ranges that work in various lighting conditions
2. **Optimized Processing Resolution**: 256x144 for better performance vs accuracy balance
3. **Enhanced Edge Detection**: Improved Sobel kernel sizes and thresholds
4. **Robust Contour Separation**: Better handling of touching samples
5. **Higher Confidence Thresholds**: 70% minimum color overlap for more reliable detection

## Setup Instructions

### 1. Upload Python Script to Limelight

1. **Access Limelight Web Interface**
   - Connect to `http://limelight.local:5801` from Driver Station
   - Navigate to the "Python" tab

2. **Create New Pipeline**
   - Click "Add Pipeline" 
   - Name it "FatDragon Sample Detection"
   - Set pipeline index to 1

3. **Upload Python Script**
   - Copy the contents of `TeamCode/src/main/python/fatdragon_sample_detection.py`
   - Paste into the Python editor
   - Click "Save"

4. **Configure Pipeline Settings**
   - Set resolution to 320x240 for optimal performance
   - Enable "Python Script" processing
   - Set exposure to manual mode with low values (-5 to -10)
   - Set gain to 0 or low values

### 2. Test the Implementation

1. **Run FatDragon Vision Test OpMode**
   ```java
   // Use FatDragonVisionTest.java
   ```

2. **Verify Detection**
   - Place samples in front of the camera
   - Check that samples are detected with high confidence (>70%)
   - Verify color classification is accurate
   - Test with different lighting conditions

### 3. Integration with Robot Code

The FatDragon vision system returns data in the same format as the previous system:

```java
// Python output array format:
// [0] Number of samples detected
// [1] X coordinate of best sample (0-256)
// [2] Y coordinate of best sample (0-144) 
// [3] Angle/orientation in degrees
// [4] Color code (1=Red, 2=Yellow, 3=Blue)
// [5] Quadrant (0=center, 1=top, 2=bottom)
// [6] Confidence percentage (0-100)
// [7] Distance estimate in inches
```

## Configuration Parameters

### HSV Color Ranges (Proven FatDragon Values)
```python
HSV_BLUE_RANGE = ([90, 120, 40], [140, 255, 255])
HSV_RED_RANGE_1 = ([0, 120, 40], [10, 255, 255])    # Lower red
HSV_RED_RANGE_2 = ([160, 120, 40], [180, 255, 255]) # Upper red  
HSV_YELLOW_RANGE = ([10, 120, 40], [30, 255, 255])
```

### Edge Detection Parameters
```python
BLUR_SIZE = 17          # FatDragon uses 17 for better edge detection
SOBEL_KERNEL = 3        # FatDragon uses 3 for sharper edges
EDGE_THRESHOLD = 50     # FatDragon threshold value
```

### Filtering Parameters
```python
MIN_CONTOUR_AREA = 200           # FatDragon uses 200
MIN_BRIGHTNESS_THRESHOLD = 60    # FatDragon uses 60
MIN_COLOR_OVERLAP = 0.7          # FatDragon uses 70% minimum
```

## Troubleshooting

### No Samples Detected
1. Check lighting conditions - ensure adequate but not excessive lighting
2. Verify HSV ranges are appropriate for your lighting
3. Check camera exposure settings (should be low for consistent colors)
4. Ensure samples are within detection range (6-100 inches)

### Poor Color Classification
1. Adjust HSV ranges in `vision_config.py` if needed
2. Check for reflections or shadows on samples
3. Verify camera white balance settings
4. Consider using lighting presets for your conditions

### Low Performance
1. Verify processing resolution is set to 256x144
2. Check Limelight CPU usage in web interface
3. Ensure proper camera settings (low exposure, low gain)
4. Monitor performance stats in telemetry

## Performance Monitoring

The FatDragon implementation includes performance tracking:

```java
// Monitor these values in telemetry:
- Total processing latency
- Individual OpenCV function timing
- Detection confidence levels
- Frame rate and CPU usage
```

## Lighting Condition Presets

The system includes presets for different lighting conditions. Uncomment the appropriate section in `vision_config.py`:

```python
# For bright field lighting:
# HSV_BLUE_RANGE = BrightLighting.HSV_BLUE_RANGE
# HSV_RED_RANGE_1 = BrightLighting.HSV_RED_RANGE_1
# HSV_RED_RANGE_2 = BrightLighting.HSV_RED_RANGE_2
# HSV_YELLOW_RANGE = BrightLighting.HSV_YELLOW_RANGE
# MIN_BRIGHTNESS_THRESHOLD = BrightLighting.MIN_BRIGHTNESS_THRESHOLD
```

## Next Steps

1. **Test thoroughly** with your specific samples and lighting conditions
2. **Fine-tune parameters** if needed for your environment
3. **Integrate with robot code** using the existing VisionSystem class
4. **Monitor performance** during practice and competition

The FatDragon-based system should provide significantly improved sample detection reliability compared to the previous implementation.
