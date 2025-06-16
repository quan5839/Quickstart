# Limelight 3A Pipeline Switching Troubleshooting Guide

## Problem: Pipeline Stuck on Index 0

If your Limelight is stuck showing "Pipeline Index: 0" even after calling `limelight.pipelineSwitch(1)`, this is a common issue with several possible causes and solutions.

## Root Causes & Solutions

### 1. **Pipeline 1 Doesn't Exist**
**Symptoms**: Pipeline always shows 0, no matter what you switch to
**Solution**: 
1. Access Limelight web interface: `http://limelight.local:5801`
2. Go to "Pipelines" tab
3. Verify Pipeline 1 exists and is properly configured
4. If not, create Pipeline 1 and upload your Python script

### 2. **Python Script Not Uploaded to Pipeline 1**
**Symptoms**: Pipeline switches to 1 but no Python output data
**Solution**:
1. Go to Limelight web interface → "Python" tab
2. Select Pipeline 1 from dropdown
3. Copy contents of `fatdragon_sample_detection.py`
4. Paste into Python editor
5. Click "Save" - **CRITICAL STEP**

### 3. **Pipeline Switch Timing Issues**
**Symptoms**: Pipeline eventually switches but takes 5-10 seconds
**Solution**: The updated `FatDragonVisionTest.java` now includes:
- Retry logic with 2-second intervals
- Pipeline switch verification
- Status feedback during switching

### 4. **Limelight Not Fully Booted**
**Symptoms**: Pipeline switching fails immediately after robot startup
**Solution**:
- Wait 30-60 seconds after robot boot before testing
- Check Limelight web interface is accessible
- Verify Limelight LED status (should be solid green when ready)

### 5. **Network/USB Connection Issues**
**Symptoms**: Intermittent pipeline switching, connection errors
**Solution**:
- Check USB connection to Control Hub
- Verify Limelight IP: `http://limelight.local:5801`
- Try power cycling the Limelight (unplug/replug USB)

## Verification Steps

### Step 1: Check Limelight Web Interface
1. Connect to `http://limelight.local:5801`
2. Go to "Pipelines" tab
3. Verify you have at least 2 pipelines:
   - Pipeline 0: Basic/Default
   - Pipeline 1: FatDragon Python Script
4. Manually switch between pipelines in web interface

### Step 2: Verify Python Script Upload
1. In web interface, go to "Python" tab
2. Select Pipeline 1 from dropdown
3. Verify the Python editor contains your FatDragon script
4. Look for the `runPipeline` function
5. Click "Save" to ensure it's properly saved

### Step 3: Test Pipeline Switching in Web Interface
1. Go to "Input" tab to see camera feed
2. Manually switch between Pipeline 0 and 1
3. Verify the pipeline index changes in the status
4. Check if Python output appears when on Pipeline 1

### Step 4: Test with Updated OpMode
1. Run the updated `FatDragonVisionTest` OpMode
2. Watch the telemetry for pipeline switching status
3. Use gamepad A/B to switch pipelines
4. Look for "✅ Successfully switched" messages

## Expected Behavior

### Pipeline 0 (Basic)
- Should show basic targeting data (TX, TY, TA)
- No Python output data
- Fast switching (< 1 second)

### Pipeline 1 (FatDragon)
- Should show Python output array with 8 values
- Sample detection data (color, position, confidence)
- May take 1-2 seconds to fully switch

## Advanced Troubleshooting

### If Pipeline Still Won't Switch

1. **Power Cycle Limelight**:
   ```
   - Unplug USB from Control Hub
   - Wait 10 seconds
   - Plug back in
   - Wait for green LED (30-60 seconds)
   ```

2. **Check Limelight Firmware**:
   - Go to web interface → "Settings" tab
   - Verify firmware is up to date
   - Consider factory reset if issues persist

3. **Verify Robot Configuration**:
   - Check `robotconfig.xml` has correct Limelight entry
   - Ensure hardware map name matches ("limelight")

4. **Test with Different Pipeline Numbers**:
   - Try switching to Pipeline 2, 3, etc.
   - If those work, Pipeline 1 may be corrupted

### Network Table Alternative (Last Resort)

If SDK pipeline switching fails completely, you can try HTTP commands:

```java
// Alternative pipeline switching via HTTP
private void switchPipelineHTTP(int pipeline) {
    // This requires additional HTTP client implementation
    // Not recommended as first solution
}
```

## Success Indicators

You'll know the pipeline switching is working when:

1. **Telemetry shows**: "✅ Successfully switched to 1"
2. **Pipeline Index**: Changes from 0 to 1 in status
3. **Python Output**: Non-null array with 8 values
4. **Sample Detection**: Shows actual detection data when samples are visible

## Next Steps

Once pipeline switching works:
1. Test sample detection with actual game pieces
2. Tune camera settings (exposure, gain) for your lighting
3. Adjust HSV ranges in `vision_config.py` if needed
4. Integrate into your main robot code

## Common Mistakes to Avoid

1. **Not saving Python script** after pasting in web interface
2. **Switching pipelines too frequently** (< 1 second intervals)
3. **Not waiting for Limelight boot** before testing
4. **Wrong pipeline index** (using 2, 3, etc. instead of 1)
5. **Missing pipeline creation** in web interface
