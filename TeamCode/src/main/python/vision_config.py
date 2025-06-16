"""
Vision Configuration Module
Centralizes all vision system parameters for easy tuning

This file contains the same parameters as VisionConstants.java but in Python format.
Modify these values to tune the vision system for your specific setup.

To use different lighting presets, uncomment the appropriate section at the bottom.
"""

import math

# ========== CAMERA PHYSICAL SETUP ==========

# Height of the camera lens from the ground in inches
CAMERA_HEIGHT_INCHES = 8.0

# Camera pitch angle in degrees (negative = tilted down)
CAMERA_PITCH_DEGREES = -15.0

# Approximate height of samples from ground in inches
SAMPLE_HEIGHT_INCHES = 1.0

# Limelight 3A Field of View specifications (hardware specs)
CAMERA_FOV_HORIZONTAL = 59.6  # degrees
CAMERA_FOV_VERTICAL = 45.7    # degrees

# ========== IMAGE PROCESSING SETTINGS ==========
# UPDATED TO FATDRAGON PROVEN WORKING VALUES

# Processing resolution - FatDragon uses 256x144 for optimal performance
PROCESSING_WIDTH = 256
PROCESSING_HEIGHT = 144

# ========== HSV COLOR RANGES ==========
# Format: ([H_min, S_min, V_min], [H_max, S_max, V_max])
# UPDATED TO FATDRAGON PROVEN WORKING VALUES

# Blue sample HSV range - FatDragon proven values
HSV_BLUE_RANGE = ([90, 120, 40], [140, 255, 255])

# Red sample HSV ranges (red wraps around in HSV) - FatDragon proven values
HSV_RED_RANGE_1 = ([0, 120, 40], [10, 255, 255])    # Lower red
HSV_RED_RANGE_2 = ([160, 120, 40], [180, 255, 255]) # Upper red

# Yellow sample HSV range - FatDragon proven values
HSV_YELLOW_RANGE = ([10, 120, 40], [30, 255, 255])

# ========== EDGE DETECTION PARAMETERS ==========
# UPDATED TO FATDRAGON PROVEN WORKING VALUES

# Gaussian blur kernel size (must be odd) - FatDragon uses 17
BLUR_SIZE = 17

# Sobel kernel size (must be odd) - FatDragon uses 3
SOBEL_KERNEL = 3

# Edge detection threshold - FatDragon uses 50
EDGE_THRESHOLD = 50

# ========== FILTERING PARAMETERS ==========
# UPDATED TO FATDRAGON PROVEN WORKING VALUES

# Contour area limits in pixels - FatDragon uses 200 minimum
MIN_CONTOUR_AREA = 200
MAX_CONTOUR_AREA = 8000

# Minimum brightness threshold (0-255) - FatDragon uses 60
MIN_BRIGHTNESS_THRESHOLD = 60

# Minimum color overlap percentage (0.0-1.0) - FatDragon uses 70%
MIN_COLOR_OVERLAP = 0.7

# ========== MULTI-SAMPLE DETECTION ==========

# Maximum number of samples to detect
MAX_SAMPLES_TO_DETECT = 5

# Minimum separation between sample centers in pixels
MIN_SAMPLE_SEPARATION = 30

# ========== QUADRANT DETECTION ==========

# Quadrant thresholds (0.0-1.0)
QUADRANT_TOP_THRESHOLD = 0.35
QUADRANT_BOTTOM_THRESHOLD = 0.65

# ========== DISTANCE CALCULATION ==========

# Distance limits in inches
MIN_DISTANCE_INCHES = 6.0
MAX_DISTANCE_INCHES = 100.0
DEFAULT_DISTANCE_INCHES = 24.0

# ========== LIGHTING CONDITION PRESETS ==========

class BrightLighting:
    """Preset for bright field lighting conditions - Based on FatDragon values"""
    HSV_BLUE_RANGE = ([90, 120, 40], [140, 255, 255])
    HSV_RED_RANGE_1 = ([0, 120, 40], [10, 255, 255])
    HSV_RED_RANGE_2 = ([160, 120, 40], [180, 255, 255])
    HSV_YELLOW_RANGE = ([10, 120, 40], [30, 255, 255])
    MIN_BRIGHTNESS_THRESHOLD = 60

class DimLighting:
    """Preset for dim/indoor lighting conditions - Based on FatDragon values"""
    HSV_BLUE_RANGE = ([85, 100, 25], [145, 255, 255])
    HSV_RED_RANGE_1 = ([0, 100, 25], [15, 255, 255])
    HSV_RED_RANGE_2 = ([155, 100, 25], [180, 255, 255])
    HSV_YELLOW_RANGE = ([8, 100, 25], [35, 255, 255])
    MIN_BRIGHTNESS_THRESHOLD = 45

class MixedLighting:
    """Preset for mixed/variable lighting conditions - Based on FatDragon values"""
    HSV_BLUE_RANGE = ([85, 80, 20], [145, 255, 255])
    HSV_RED_RANGE_1 = ([0, 80, 20], [18, 255, 255])
    HSV_RED_RANGE_2 = ([150, 80, 20], [180, 255, 255])
    HSV_YELLOW_RANGE = ([8, 80, 20], [38, 255, 255])
    MIN_BRIGHTNESS_THRESHOLD = 40

# ========== HELPER FUNCTIONS ==========

def pixels_to_angles(pixel_x, pixel_y):
    """Convert pixel coordinates to field angles"""
    center_x = PROCESSING_WIDTH / 2.0
    center_y = PROCESSING_HEIGHT / 2.0
    
    offset_x = pixel_x - center_x
    offset_y = pixel_y - center_y
    
    angle_x = (offset_x / PROCESSING_WIDTH) * CAMERA_FOV_HORIZONTAL
    angle_y = (offset_y / PROCESSING_HEIGHT) * CAMERA_FOV_VERTICAL
    
    return angle_x, angle_y

def calculate_distance(pixel_x, pixel_y):
    """Calculate distance to sample using camera geometry"""
    try:
        angle_x, angle_y = pixels_to_angles(pixel_x, pixel_y)
        
        # Adjust for camera pitch
        effective_angle = math.radians(angle_y + CAMERA_PITCH_DEGREES)
        
        # Distance calculation
        if abs(effective_angle) > 0.01:
            distance = (CAMERA_HEIGHT_INCHES - SAMPLE_HEIGHT_INCHES) / math.tan(abs(effective_angle))
            return max(MIN_DISTANCE_INCHES, min(distance, MAX_DISTANCE_INCHES))
        else:
            return DEFAULT_DISTANCE_INCHES
    except:
        return DEFAULT_DISTANCE_INCHES

# ========== LIGHTING PRESET SELECTION ==========

# UNCOMMENT ONE OF THESE SECTIONS TO USE A LIGHTING PRESET:

# # For bright field lighting:
# HSV_BLUE_RANGE = BrightLighting.HSV_BLUE_RANGE
# HSV_RED_RANGE_1 = BrightLighting.HSV_RED_RANGE_1
# HSV_RED_RANGE_2 = BrightLighting.HSV_RED_RANGE_2
# HSV_YELLOW_RANGE = BrightLighting.HSV_YELLOW_RANGE
# MIN_BRIGHTNESS_THRESHOLD = BrightLighting.MIN_BRIGHTNESS_THRESHOLD

# # For dim/indoor lighting:
# HSV_BLUE_RANGE = DimLighting.HSV_BLUE_RANGE
# HSV_RED_RANGE_1 = DimLighting.HSV_RED_RANGE_1
# HSV_RED_RANGE_2 = DimLighting.HSV_RED_RANGE_2
# HSV_YELLOW_RANGE = DimLighting.HSV_YELLOW_RANGE
# MIN_BRIGHTNESS_THRESHOLD = DimLighting.MIN_BRIGHTNESS_THRESHOLD

# # For mixed/variable lighting:
# HSV_BLUE_RANGE = MixedLighting.HSV_BLUE_RANGE
# HSV_RED_RANGE_1 = MixedLighting.HSV_RED_RANGE_1
# HSV_RED_RANGE_2 = MixedLighting.HSV_RED_RANGE_2
# HSV_YELLOW_RANGE = MixedLighting.HSV_YELLOW_RANGE
# MIN_BRIGHTNESS_THRESHOLD = MixedLighting.MIN_BRIGHTNESS_THRESHOLD

# ========== CONFIGURATION VALIDATION ==========

def validate_config():
    """Validate configuration parameters"""
    errors = []
    
    # Check HSV ranges
    for name, hsv_range in [
        ("BLUE", HSV_BLUE_RANGE),
        ("RED_1", HSV_RED_RANGE_1),
        ("RED_2", HSV_RED_RANGE_2),
        ("YELLOW", HSV_YELLOW_RANGE)
    ]:
        min_vals, max_vals = hsv_range
        if len(min_vals) != 3 or len(max_vals) != 3:
            errors.append(f"{name} HSV range must have 3 values each for min and max")
        if min_vals[0] < 0 or max_vals[0] > 180:
            errors.append(f"{name} Hue values must be 0-180")
        if any(v < 0 or v > 255 for v in min_vals[1:] + max_vals[1:]):
            errors.append(f"{name} Saturation/Value must be 0-255")
    
    # Check other parameters
    if BLUR_SIZE % 2 == 0:
        errors.append("BLUR_SIZE must be odd")
    if SOBEL_KERNEL % 2 == 0:
        errors.append("SOBEL_KERNEL must be odd")
    if not (0.0 <= MIN_COLOR_OVERLAP <= 1.0):
        errors.append("MIN_COLOR_OVERLAP must be 0.0-1.0")
    if not (0.0 <= QUADRANT_TOP_THRESHOLD <= 1.0):
        errors.append("QUADRANT_TOP_THRESHOLD must be 0.0-1.0")
    if not (0.0 <= QUADRANT_BOTTOM_THRESHOLD <= 1.0):
        errors.append("QUADRANT_BOTTOM_THRESHOLD must be 0.0-1.0")
    
    return errors

# Validate configuration on import
_validation_errors = validate_config()
if _validation_errors:
    print("Vision Configuration Errors:")
    for error in _validation_errors:
        print(f"  - {error}")

# ========== TUNING EXAMPLES ==========
# Uncomment and modify these lines to tune detection:

# Example: To make detection more sensitive to smaller samples
# MIN_CONTOUR_AREA = 150

# Example: To detect in darker conditions
# MIN_BRIGHTNESS_THRESHOLD = 45

# Example: To be less strict about color matching
# MIN_COLOR_OVERLAP = 0.6

# Example: To adjust blue detection range for your lighting
# HSV_BLUE_RANGE = ([85, 100, 30], [145, 255, 255])

# Example: To make edge detection more sensitive
# EDGE_THRESHOLD = 40

# Example: To adjust processing resolution (must match in fatdragon_sample_detection.py)
# PROCESSING_WIDTH = 320
# PROCESSING_HEIGHT = 240
