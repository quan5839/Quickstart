# Limelight 3A Python Script Loading Guide

## The Problem: Python Scripts Not Running Automatically

Based on official Limelight 3A documentation and your experience, Python scripts must be **manually saved in the Limelight web interface** for each pipeline to work properly. This is a known requirement, not a bug.

## Why This Happens

1. **Limelight 3A Architecture**: Python scripts are stored and executed on the Limelight device itself, not in your robot code
2. **Pipeline Switching**: When you switch pipelines via robot code, Limelight loads the Python script associated with that pipeline
3. **Manual Save Requirement**: The web interface requires you to manually click "Save" for each pipeline to properly store the Python script

## Step-by-Step Solution

### Step 1: Access Limelight Web Interface
1. Connect to Limelight via USB or Ethernet
2. Open web browser and navigate to `http://limelight.local:5801`
3. If that doesn't work, try `http://172.29.0.1:5801` (USB) or your Limelight's IP address

### Step 2: Upload Python Scripts to Each Pipeline

#### For Pipeline 0 (Yellow Detection):
1. Click on "Python" tab in web interface
2. Select "Pipeline 0" from the dropdown
3. Copy the entire content of `limelightyellow_llm_debug_latest.py`
4. Paste it into the Python editor
5. **CRITICAL**: Click "Save" button
6. Verify the `runPipeline(frame, llrobot)` function is present

#### For Pipeline 1 (Blue Detection):
1. Select "Pipeline 1" from the dropdown
2. Copy the entire content of `limelightblue_llm_debug_latest.py`
3. Paste it into the Python editor
4. **CRITICAL**: Click "Save" button
5. Verify the `runPipeline(frame, llrobot)` function is present

#### For Pipeline 2 (Red Detection):
1. Select "Pipeline 2" from the dropdown
2. Copy the entire content of `limelightred_llm_debug_latest.py`
3. Paste it into the Python editor
4. **CRITICAL**: Click "Save" button
5. Verify the `runPipeline(frame, llrobot)` function is present

### Step 3: Verify Script Loading

#### Using Enhanced Pipeline Test OpMode:
1. Run "Enhanced Pipeline Test" from Driver Station
2. Press Y button to run comprehensive validation
3. Check that all pipelines show "✅ LOADED" status
4. If any show "❌ NOT LOADED", repeat Step 2 for that pipeline

#### Manual Verification in Web Interface:
1. Go to "Input" tab to see camera feed
2. Manually switch between Pipeline 0, 1, and 2 using dropdown
3. Verify you see Python output data in the results
4. Check that pipeline switching works correctly

## Common Issues and Solutions

### Issue 1: "❌ NOT LOADED" Status
**Cause**: Python script not properly saved in web interface
**Solution**: 
- Go back to Python tab
- Select the problematic pipeline
- Re-paste the script content
- Click "Save" again
- Verify `runPipeline` function exists

### Issue 2: Pipeline Index Mismatch
**Cause**: Pipeline switching not working properly
**Solution**:
- Check Limelight network connection
- Restart Limelight device
- Verify pipeline exists in web interface
- Check for firmware updates

### Issue 3: No Python Output Data
**Cause**: Script has syntax errors or missing `runPipeline` function
**Solution**:
- Check Python tab for error messages
- Verify script has proper `runPipeline(frame, llrobot)` function
- Check that function returns `(contour, image, llpython_array)`
- Validate Python syntax

### Issue 4: Scripts Work in Web Interface but Not in Robot Code
**Cause**: Different pipeline being used than expected
**Solution**:
- Use Enhanced Pipeline Test to verify actual pipeline index
- Check that `limelight.pipelineSwitch(index)` is being called
- Verify pipeline index matches between robot code and web interface

## Best Practices

### 1. Always Use Enhanced Pipeline Test
- Run comprehensive validation after any changes
- Monitor Python script status continuously
- Use diagnostics to troubleshoot issues

### 2. Systematic Script Upload
- Upload scripts one at a time
- Test each pipeline individually
- Verify functionality before moving to next pipeline

### 3. Version Control for Python Scripts
- Keep Python scripts in your repository
- Document which script goes to which pipeline
- Use consistent naming conventions

### 4. Regular Validation
- Test Python scripts after any Limelight firmware updates
- Verify scripts after power cycles
- Check script status before competitions

## Enhanced Pipeline Test Features

The new `EnhancedPipelineTestTeleop` provides:

### Automatic Detection
- ✅ Detects if Python scripts are properly loaded
- ✅ Validates pipeline switching functionality
- ✅ Monitors script execution in real-time

### Comprehensive Diagnostics
- **A Button**: Detailed diagnostics with troubleshooting steps
- **Y Button**: Full validation of all pipelines
- **B Button**: Capture debug snapshots

### Clear Status Indicators
- ✅ Green checkmarks for working scripts
- ❌ Red X for non-working scripts
- ⏳ Clock for validation in progress
- ⚠️ Warning for detected issues

### Troubleshooting Guidance
- Step-by-step instructions when problems detected
- Clear error messages with solutions
- Links to relevant documentation

## Technical Details

### Python Script Requirements
```python
def runPipeline(frame, llrobot):
    # Your detection logic here
    largest_contour = np.array([[]])  # Largest detected contour
    processed_frame = frame.copy()    # Processed image for display
    llpython = [0, 0, 0, 0, 0, 0, 0, 0]  # 8-element output array
    
    return largest_contour, processed_frame, llpython
```

### Expected Output Format
- `llpython[0]`: Number of samples detected (0 or 1)
- `llpython[1]`: Sample X position (pixels)
- `llpython[2]`: Sample Y position (pixels)
- `llpython[3]`: Sample angle (degrees)
- `llpython[4]`: Color code (1=Red, 2=Yellow, 3=Blue)
- `llpython[5]`: Quadrant (0=none, 1=top, 2=bottom)
- `llpython[6]`: Confidence (0-100)
- `llpython[7]`: Sample area (pixels)

### Robot Code Integration
```java
// Get Python output
double[] pythonOutput = result.getPythonOutput();
if (pythonOutput != null && pythonOutput.length >= 8) {
    boolean sampleDetected = pythonOutput[0] > 0;
    double sampleX = pythonOutput[1];
    double sampleY = pythonOutput[2];
    // ... use other values as needed
}
```

## Conclusion

The key to successful Limelight 3A Python script integration is understanding that:

1. **Manual saving is required** - This is by design, not a bug
2. **Each pipeline needs individual setup** - Scripts must be uploaded to each pipeline separately
3. **Validation is essential** - Always verify scripts are working before relying on them
4. **Enhanced tools help** - Use the Enhanced Pipeline Test for better debugging

By following this guide and using the Enhanced Pipeline Test OpMode, you should be able to get your Python scripts running reliably without needing to manually save them every time after the initial setup.
