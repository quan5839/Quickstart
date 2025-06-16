# FatDragon on Pipeline 0 - Simple Setup Guide

## Why Use Pipeline 0?

Using Pipeline 0 for your FatDragon script eliminates pipeline switching issues entirely. This is the **simplest and most reliable** approach.

## Setup Steps

### 1. Access Limelight Web Interface
1. Connect to `http://limelight.local:5801`
2. Navigate to the **"Python"** tab

### 2. Select Pipeline 0
1. In the Python tab dropdown, select **"Pipeline 0"**
2. This is usually already selected by default

### 3. Upload FatDragon Script
1. **Clear existing code** in the Python editor (if any)
2. **Copy** the entire contents of `TeamCode/src/main/python/fatdragon_sample_detection.py`
3. **Paste** into the Python editor
4. **Click "Save"** - This is critical!

### 4. Verify Upload
1. Refresh the page and check that your script is still there
2. Look for the `runPipeline` function in the editor
3. The script should be several hundred lines long

## Benefits of Pipeline 0 Approach

✅ **No pipeline switching needed** - Always uses Pipeline 0
✅ **Eliminates switching bugs** - No more "stuck on pipeline 0" issues  
✅ **Simpler code** - No complex switching logic required
✅ **Faster startup** - No waiting for pipeline switches
✅ **More reliable** - One less thing that can go wrong

## Updated Code Changes

The following files have been updated to use Pipeline 0:

### FatDragonVisionTest.java
- Removed pipeline switching controls
- Always uses Pipeline 0
- Simplified troubleshooting

### VisionSystem.java  
- Defaults to Pipeline 0
- Color recognition uses Pipeline 0
- No more pipeline switching logic

## Testing Your Setup

### 1. Run FatDragonVisionTest
1. Deploy and run the updated `FatDragonVisionTest` OpMode
2. Look for these success indicators:
   - **Pipeline Index: 0** (should show immediately)
   - **✅ Python Script: Loaded and running on Pipeline 0**
   - **Python output data** (8-value array)

### 2. Check for Sample Detection
1. Place yellow game pieces in camera view
2. Look for detection data:
   - **🎯 SAMPLES DETECTED: X samples**
   - **Position, angle, confidence data**
   - **Raw Python Data array**

### 3. Verify Camera Feed
1. In Limelight web interface, go to **"Input"** tab
2. You should see the camera feed with detection overlays
3. Yellow samples should have green contours and labels

## Troubleshooting

### If No Python Output
**Problem**: Pipeline shows 0 but no Python data
**Solution**: 
1. Go back to web interface → Python tab
2. Verify script is actually saved (refresh page to check)
3. Look for syntax errors in the Python console
4. Try uploading the script again

### If No Sample Detection
**Problem**: Python runs but no samples detected
**Solution**:
1. Check lighting conditions
2. Verify yellow samples are in view
3. Adjust camera exposure/gain settings
4. Check HSV ranges in the Python script

### If Camera Feed Issues
**Problem**: No camera feed or poor quality
**Solution**:
1. Check USB connection to Control Hub
2. Power cycle the Limelight
3. Verify camera settings in web interface

## Camera Settings for Pipeline 0

Recommended settings in Limelight web interface:

### Input Tab
- **Resolution**: 320x240 (for performance)
- **Exposure**: Manual, -5 to -10
- **Gain**: 0 to 5
- **White Balance**: Auto or manual tuning

### Output Tab  
- **Stream**: Enabled for debugging
- **Snapshot**: Enabled for tuning

## Next Steps

Once Pipeline 0 is working:

1. **Test with real game pieces** in various lighting conditions
2. **Tune camera settings** for your specific environment
3. **Adjust HSV ranges** in `vision_config.py` if needed
4. **Integrate into your main robot code** using `VisionSystem.java`

## Advantages Over Pipeline 1 Approach

| Pipeline 0 | Pipeline 1 |
|------------|------------|
| ✅ Always active | ❌ Requires switching |
| ✅ No switching delays | ❌ 1-2 second switch time |
| ✅ Simpler code | ❌ Complex switching logic |
| ✅ More reliable | ❌ Can get stuck switching |
| ✅ Faster startup | ❌ Slower initialization |

## Important Notes

- **Pipeline 0 is the default** - Limelight always starts with Pipeline 0
- **No basic targeting lost** - FatDragon script provides both detection AND basic targeting
- **Backward compatible** - Existing code will work without changes
- **Performance optimized** - No overhead from pipeline switching

This approach eliminates the most common source of Limelight vision issues while providing the same functionality!
