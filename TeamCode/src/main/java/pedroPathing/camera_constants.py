"""
Camera Configuration Constants for Limelight 3A Vision System

This module contains all camera-related constants including physical mounting
parameters, field of view specifications, and sample detection settings.

These constants are used by the Python vision pipelines for accurate
distance calculations and robot-relative positioning.

This file mirrors the Java CameraConstants.java file for consistency.
"""

import math

# ========== LIMELIGHT 3A PHYSICAL MOUNTING ==========

# Height of the Limelight 3A camera above the ground (inches)
# Measure from ground to camera lens center
CAMERA_HEIGHT = 8.0

# Camera tilt angle downward from horizontal (degrees)
# Positive values = tilted down, Negative values = tilted up
CAMERA_TILT_ANGLE = 25.0

# Camera offset from robot center - forward/backward direction (inches)
# Positive = camera is forward of robot center
# Negative = camera is behind robot center
CAMERA_OFFSET_X = 0.0

# Camera offset from robot center - left/right direction (inches)
# Positive = camera is to the right of robot center
# Negative = camera is to the left of robot center
CAMERA_OFFSET_Y = 0.0

# ========== LIMELIGHT 3A SPECIFICATIONS ==========

# Limelight 3A horizontal field of view (degrees)
# This is a fixed specification of the Limelight 3A
CAMERA_FOV_HORIZONTAL = 59.6

# Limelight 3A vertical field of view (degrees)
# This is a fixed specification of the Limelight 3A
CAMERA_FOV_VERTICAL = 45.7

# Camera resolution width (pixels)
CAMERA_WIDTH = 320

# Camera resolution height (pixels)
CAMERA_HEIGHT_PIXELS = 240

# Camera frame rate (fps)
CAMERA_FPS = 120

# ========== GAME PIECE SPECIFICATIONS ==========

# Height of samples above the ground (inches)
# This is the height of the sample center when sitting on the field
SAMPLE_HEIGHT = 1.0

# Approximate diameter of samples (inches)
# Used for size validation and distance estimation
SAMPLE_DIAMETER = 3.5

# ========== DISTANCE CALCULATION LIMITS ==========

# Minimum detectable distance (inches)
# Samples closer than this may not be accurately detected
MIN_DETECTION_DISTANCE = 2.0

# Maximum detectable distance (inches)
# Samples farther than this may not be accurately detected
MAX_DETECTION_DISTANCE = 150.0

# Default distance when calculation fails (inches)
DEFAULT_DISTANCE = 50.0

# ========== CAMERA SETTINGS ==========

# Limelight 3A camera exposure setting (.01 ms units)
# Current setting: 3300 = 33.00 ms
CAMERA_EXPOSURE = 3300

# Limelight 3A black level offset
CAMERA_BLACK_LEVEL_OFFSET = 4

# Limelight 3A sensor gain
CAMERA_SENSOR_GAIN = 19

# Limelight 3A red balance
CAMERA_RED_BALANCE = 1802

# Limelight 3A blue balance
CAMERA_BLUE_BALANCE = 1637

# ========== UTILITY FUNCTIONS ==========

def calculate_distance_to_sample(center_x, center_y, frame_width, frame_height):
    """Calculate distance to sample using camera geometry and physical constants"""
    try:
        # Convert pixel coordinates to normalized coordinates (-1 to 1)
        center_x_norm = (center_x - frame_width/2) / (frame_width/2)
        center_y_norm = (center_y - frame_height/2) / (frame_height/2)

        # Calculate angles from camera center
        angle_x = center_x_norm * (CAMERA_FOV_HORIZONTAL / 2)  # Horizontal angle
        angle_y = center_y_norm * (CAMERA_FOV_VERTICAL / 2)    # Vertical angle

        # Adjust for camera tilt
        effective_angle = math.radians(angle_y - CAMERA_TILT_ANGLE)

        # Calculate distance using trigonometry
        if abs(effective_angle) > 0.001:  # Avoid division by zero
            height_diff = CAMERA_HEIGHT - SAMPLE_HEIGHT
            distance = height_diff / math.tan(abs(effective_angle))
            return max(MIN_DETECTION_DISTANCE, min(distance, MAX_DETECTION_DISTANCE))
        else:
            return DEFAULT_DISTANCE  # Default distance for very distant samples
    except:
        return DEFAULT_DISTANCE  # Default distance on error

def calculate_robot_relative_position(center_x, center_y, frame_width, frame_height):
    """Calculate sample position relative to robot center"""
    try:
        # Get distance to sample
        distance = calculate_distance_to_sample(center_x, center_y, frame_width, frame_height)
        
        # Convert pixel coordinates to normalized coordinates
        center_x_norm = (center_x - frame_width/2) / (frame_width/2)
        
        # Calculate horizontal angle from camera center
        angle_x = center_x_norm * (CAMERA_FOV_HORIZONTAL / 2)
        angle_x_rad = math.radians(angle_x)
        
        # Calculate sample position relative to camera
        sample_x_camera = distance * math.sin(angle_x_rad)  # Left/right from camera
        sample_y_camera = distance * math.cos(angle_x_rad)  # Forward from camera
        
        # Adjust for camera offset from robot center
        sample_x_robot = sample_x_camera + CAMERA_OFFSET_Y  # Left/right from robot center
        sample_y_robot = sample_y_camera + CAMERA_OFFSET_X  # Forward/backward from robot center
        
        return {
            'distance': distance,
            'x_robot': sample_x_robot,      # Left/right from robot center (+ = right)
            'y_robot': sample_y_robot,      # Forward/backward from robot center (+ = forward)
            'angle_to_sample': angle_x      # Angle to turn to face sample
        }
    except:
        return {
            'distance': DEFAULT_DISTANCE,
            'x_robot': 0.0,
            'y_robot': DEFAULT_DISTANCE,
            'angle_to_sample': 0.0
        }

def get_camera_info_string():
    """Get a formatted string with camera configuration info"""
    return f"Cam: H={CAMERA_HEIGHT}in T={CAMERA_TILT_ANGLE}° Offset=({CAMERA_OFFSET_X},{CAMERA_OFFSET_Y})"

# ========== COORDINATE SYSTEM NOTES ==========
"""
Robot Coordinate System:
- X-axis: Forward/Backward (+ = forward, - = backward)
- Y-axis: Left/Right (+ = right, - = left)
- Origin: Robot center

Camera Coordinate System:
- Pixel (0,0) is top-left corner
- Pixel (320,240) is bottom-right corner
- Center pixel is (160,120)

Field Coordinate System:
- Follows FTC field coordinate conventions
- Can be converted from robot-relative coordinates
"""

# ========== USAGE EXAMPLES ==========
"""
Pipeline Usage:

# Import constants
from camera_constants import *

# Calculate distance
distance = calculate_distance_to_sample(center_x, center_y, frame_width, frame_height)

# Calculate robot-relative position
robot_pos = calculate_robot_relative_position(center_x, center_y, frame_width, frame_height)

# Access individual constants
camera_height = CAMERA_HEIGHT
tilt_angle = CAMERA_TILT_ANGLE

# Get camera info for display
info_text = get_camera_info_string()
"""
