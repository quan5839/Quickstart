# Limelight 3A Camera Settings for Sample Detection

## FatDragon Proven Working Settings

Based on FTC team 12527 FatDragon's proven working implementation that achieved 5-6 sample autos consistently.

### Core Camera Settings

**From FatDragon's cvlimelight4.py:**
```python
# Camera settings
CAMERA_WIDTH = 320
CAMERA_HEIGHT = 240
CAMERA_FPS = 120

# Camera exposure settings
AUTO_EXPOSURE = 0.25
EXPOSURE = -5

# Camera gain settings
GAIN = 0  # Note: This is overridden by manual tuning

# Camera white balance settings
AUTO_WB = 0
WB_RED = 1.0
WB_GREEN = 1.0
WB_BLUE = 1.0
```

### Critical Limelight 3A Web Interface Settings

#### 1. **Sensor Gain: MAXIMUM**
- **Setting**: Push sensor gain to maximum value
- **Purpose**: "Really helps with blurring and blending the unwanted lines and features on top of the samples"
- **Result**: Creates clean frames where samples are super bright and everything else is black

#### 2. **Resolution: LOWEST**
- **Setting**: Use lowest available resolution
- **Purpose**: Better performance and processing speed
- **FatDragon uses**: 320x240 capture, 256x144 processing

#### 3. **Exposure: Environment-Dependent**
- **Starting point**: -5 (from FatDragon code)
- **Tuning**: Adjust based on lighting conditions

#### 4. **Black Level Offset: Tuned for Separation**
- **Purpose**: Controls sample separation detection
- **Tuning**: Reduce if touching samples are detected as one

### Tuning Process

#### Step 1: Load FatDragon .vpr Config
1. Download the appropriate .vpr file from FatDragon's "final pipelines" folder
2. Upload to Limelight 3A web interface
3. Choose config based on sample color you want to detect

#### Step 2: Environment Tuning
**Target Image Quality:**
- Colors should be "really vibrant and uniform"
- "Little to no noise on the samples"
- Target samples should be "super bright"
- Background should be "black"

**If Image is Too Dark:**
- Increase exposure
- Increase sensor gain (if not already at max)
- Add LED lighting to robot (FatDragon uses LED lights)

**If Image is Too Bright:**
- Reduce exposure
- Reduce black level offset
- Symptoms: Touching samples detected as one sample

#### Step 3: Verify Detection
**Good Detection Indicators:**
- Individual samples clearly separated
- Consistent color detection
- Minimal noise/false positives
- Stable detection across different angles

### Reference Images from FatDragon

**Properly Tuned (Good):**
- Vibrant, uniform colors
- Clean sample edges
- Dark background
- No flickering

**Too Dark (Bad):**
- Colors flickering
- Poor contrast
- Inconsistent detection

**Too Bright (Bad):**
- Touching samples detected as one
- Oversaturated colors
- Loss of edge definition

### Hardware Recommendations

#### Lighting
- **Add LED lighting**: FatDragon specifically uses LED lights on their robot
- **Consistent lighting**: Helps maintain stable detection across different field conditions

#### Camera Mounting
- **Stable mount**: Minimize vibration and movement
- **Optimal angle**: Test different mounting angles for best sample visibility
- **Height considerations**: Balance between field of view and detection accuracy

### Color-Specific Settings

FatDragon provides separate .vpr configs for each color:

#### Yellow Samples (Most Common)
- **Config**: Use yellow.vpr from FatDragon
- **HSV Range**: [10, 120, 50] to [30, 255, 255]
- **Notes**: Most tested and reliable

#### Blue Samples
- **Config**: Use blue.vpr from FatDragon
- **HSV Range**: [90, 120, 40] to [140, 255, 255]

#### Red Samples
- **Config**: Use red.vpr from FatDragon
- **HSV Range**: [0, 120, 40] to [10, 255, 255] + [170, 120, 40] to [180, 255, 255]

### Troubleshooting

#### Poor Detection
1. **Check lighting**: Ensure adequate, consistent lighting
2. **Verify .vpr config**: Make sure correct config is loaded
3. **Tune exposure**: Adjust for current lighting conditions
4. **Check camera focus**: Ensure Limelight 3A is properly focused

#### False Positives
1. **Increase black level offset**: Reduces noise detection
2. **Adjust HSV ranges**: Fine-tune color detection ranges
3. **Check background**: Ensure field elements aren't interfering

#### Touching Samples Detected as One
1. **Reduce exposure**: Prevent oversaturation
2. **Reduce black level offset**: Improve edge definition
3. **Increase contrast**: Adjust sensor gain if needed

### Performance Notes

- **Processing Resolution**: FatDragon uses 256x144 for optimal performance
- **Frame Rate**: 120 FPS for responsive detection
- **Update Rate**: Balance between accuracy and performance

### Integration with Robot Code

See `FATDRAGON_VISION_SETUP.md` for complete integration instructions with the updated Python pipeline that includes the critical gray boosting step.

## Summary

The key insight from FatDragon is that **maximum sensor gain** combined with proper exposure tuning creates the ideal conditions for sample detection. Their proven settings have been tested in competition and work reliably for 5-6 sample autonomous routines.
