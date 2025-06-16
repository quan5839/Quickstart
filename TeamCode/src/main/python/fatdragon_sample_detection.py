"""
Enhanced FatDragon Sample Detection Pipeline for Limelight 3A
Integrated from proven working final pipeline implementations

Features:
- Multi-color detection (Blue, Red, Yellow) in single pipeline
- Optimized HSV color ranges from final debug implementations
- Streamlined edge detection with Sobel operators
- Robust contour separation for touching samples
- Distance calculation and quadrant detection
- Debug capabilities for pipeline tuning
- Performance optimized for real-time detection

Author: Based on FTC-12527-FatDragon final pipeline implementations
Version: 3.0 - Integrated Multi-Color Detection
"""

import cv2
import numpy as np
import math

# ========== OPTIMIZED CONFIGURATION FROM FINAL PIPELINES ==========

# Camera settings
CAMERA_WIDTH = 320
CAMERA_HEIGHT = 240
CAMERA_FPS = 120

# HSV color ranges - From final pipeline implementations
HSV_BLUE_RANGE = ([80, 60, 100], [110, 255, 255])
HSV_RED_RANGE_1 = ([0, 120, 40], [10, 255, 255])    # Lower red
HSV_RED_RANGE_2 = ([170, 120, 40], [180, 255, 255]) # Upper red
HSV_YELLOW_RANGE = ([15, 60, 100], [80, 255, 255])

# Filtering parameters - From final pipeline implementations
SMALL_CONTOUR_AREA = 300
MIN_BRIGHTNESS_THRESHOLD = 50

# Aspect ratio filtering
MIN_ASPECT_RATIO = 1.5  # Minimum width/height ratio
MAX_ASPECT_RATIO = 6.0  # Maximum width/height ratio

# Color codes for llpython output
COLOR_CODES = {
    'BLUE': 1,
    'RED': 2,
    'YELLOW': 3,
    'UNKNOWN': 0
}

def calculate_angle(contour):
    """Calculate angle of contour using ellipse fitting"""
    if len(contour) < 5:
        return 0
    (x, y), (MA, ma), angle = cv2.fitEllipse(contour)
    return angle

def draw_info(image, color, angle, center, index, area):
    """Draw sample information with color-coded visualization"""
    # Color-coded text based on sample color
    text_color = (255, 0, 0) if color == "BLUE" else (0, 0, 255) if color == "RED" else (0, 255, 0)

    cv2.putText(image, f"#{index}: {color}", (center[0] - 40, center[1] - 60),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, text_color, 2)
    cv2.putText(image, f"Angle: {angle:.2f}", (center[0] - 40, center[1] - 40),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, text_color, 2)
    cv2.putText(image, f"Area: {area:.2f}", (center[0] - 40, center[1] - 20),
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, text_color, 2)
    cv2.circle(image, center, 5, text_color, -1)
    cv2.line(image, center,
             (int(center[0] + 50 * math.cos(math.radians(90 - angle))),
              int(center[1] - 50 * math.sin(math.radians(90 - angle)))),
             text_color, 2)

def separate_touching_contours(contour, min_area_ratio=0.15):
    """Separate touching contours using distance transform"""
    x, y, w, h = cv2.boundingRect(contour)
    mask = np.zeros((h, w), dtype=np.uint8)
    shifted_contour = contour - [x, y]
    cv2.drawContours(mask, [shifted_contour], -1, 255, -1)

    original_area = cv2.contourArea(contour)
    max_contours = []
    max_count = 1

    dist_transform = cv2.distanceTransform(mask, cv2.DIST_L2, 3)
    for threshold in np.linspace(0.1, 0.9, 9):
        _, thresh = cv2.threshold(dist_transform, threshold * dist_transform.max(), 255, 0)
        thresh = np.uint8(thresh)
        contours, _ = cv2.findContours(thresh, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)

        valid_contours = [c for c in contours if cv2.contourArea(c) > original_area * min_area_ratio]
        if len(valid_contours) > max_count:
            max_count = len(valid_contours)
            max_contours = valid_contours

    if max_contours:
        return [c + [x, y] for c in max_contours]
    return [contour]

def pipeline_debug_return(frame):
    """Debug return function for pipeline debugging"""
    return None, None, None, True, frame

def process_color(frame, mask):
    """Process a single color mask using optimized edge detection pipeline"""
    debug_info = None
    kernel = np.ones((5, 5), np.uint8)

    # Apply mask to frame
    masked_frame = cv2.bitwise_and(frame, frame, mask=mask)
    gray_masked = cv2.cvtColor(masked_frame, cv2.COLOR_BGR2GRAY)

    # Gaussian blur for noise reduction
    blurred = cv2.GaussianBlur(gray_masked, (3, 3), 0)

    # Sobel edge detection
    sobelx = cv2.Sobel(blurred, cv2.CV_32F, 1, 0, ksize=1)
    sobely = cv2.Sobel(blurred, cv2.CV_32F, 0, 1, ksize=1)

    # Calculate magnitude and normalize
    magnitude = np.sqrt(sobelx**2 + sobely**2)
    if np.max(magnitude) > 0:
        magnitude = np.uint8(magnitude * 255 / np.max(magnitude))
    else:
        magnitude = np.uint8(magnitude)

    # Threshold and morphological operations
    _, edges = cv2.threshold(magnitude, 50, 255, cv2.THRESH_BINARY)
    edges = cv2.morphologyEx(edges, cv2.MORPH_CLOSE, kernel)
    edges = cv2.dilate(edges, np.ones((3, 3), np.uint8), iterations=1)
    edges = cv2.bitwise_not(edges)
    edges = cv2.bitwise_and(edges, edges, mask=mask)
    edges = cv2.GaussianBlur(edges, (3, 3), 0)

    # Find contours
    contours, hierarchy = cv2.findContours(edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    return contours, hierarchy, gray_masked, False, debug_info

def determine_quadrant(center_y, frame_height):
    """Determine which quadrant the sample is in"""
    relative_y = center_y / frame_height
    if relative_y < 0.35:
        return 1  # Top quadrant
    elif relative_y > 0.65:
        return 2  # Bottom quadrant
    else:
        return 0  # Middle (no specific quadrant)

def calculate_distance_to_sample(center_x, center_y, frame_width, frame_height):
    """Calculate distance to sample using camera geometry for Limelight 3A"""
    try:
        # Convert pixel coordinates to normalized coordinates
        center_x_norm = (center_x - frame_width/2) / frame_width
        center_y_norm = (center_y - frame_height/2) / frame_height

        # Limelight 3A FOV angles
        angle_x = center_x_norm * 59.6  # Horizontal FOV
        angle_y = center_y_norm * 45.7  # Vertical FOV

        # Distance estimation with camera geometry
        camera_tilt = 15.0  # Degrees downward tilt
        effective_angle = math.radians(angle_y - camera_tilt)

        if abs(effective_angle) > 0.005:
            camera_height = 8.0  # inches
            sample_height = 1.0  # inches
            distance = (camera_height - sample_height) / math.tan(abs(effective_angle))
            return max(4.0, min(distance, 120.0))  # Range: 4" to 120"
        else:
            return 36.0  # Default distance for very distant samples
    except:
        return 36.0  # Default distance on error

def debug_return(frame):
    """Debug return function for error handling"""
    return np.array([[]]), frame, [0, 0, 0, 0, 0, 0, 0, 0]

def process_contours(contours, gray_image, color_name, frame_width, frame_height):
    """Process contours for a specific color and return valid samples"""
    valid_samples = []

    for i, contour in enumerate(contours):
        # Filter by area
        if cv2.contourArea(contour) < SMALL_CONTOUR_AREA:
            continue

        # Check aspect ratio
        rect = cv2.minAreaRect(contour)
        width = max(rect[1])
        height = min(rect[1])
        if width == 0 or height == 0:
            continue

        aspect_ratio = width / height
        if aspect_ratio < MIN_ASPECT_RATIO or aspect_ratio > MAX_ASPECT_RATIO:
            continue

        # Separate touching contours
        for sep_contour in separate_touching_contours(contour):
            # Check brightness
            mask = np.zeros(gray_image.shape, dtype=np.uint8)
            cv2.drawContours(mask, [sep_contour], -1, 255, -1)
            avg_brightness = cv2.mean(gray_image, mask=mask)[0]

            if avg_brightness < MIN_BRIGHTNESS_THRESHOLD:
                continue

            # Calculate sample properties
            M = cv2.moments(sep_contour)
            if M["m00"] == 0:
                continue

            center_x = int(M["m10"] / M["m00"])
            center_y = int(M["m01"] / M["m00"])
            center = (center_x, center_y)
            area = cv2.contourArea(sep_contour)
            angle = calculate_angle(sep_contour)

            # Create sample data
            sample = {
                'contour': sep_contour,
                'center': center,
                'area': area,
                'angle': angle,
                'color': color_name,
                'confidence': 0.85,
                'quadrant': determine_quadrant(center_y, frame_height),
                'distance': calculate_distance_to_sample(center_x, center_y, frame_width, frame_height),
                'brightness': avg_brightness,
                'index': i + 1
            }

            valid_samples.append(sample)

    return valid_samples

def runPipeline(frame, llrobot):
    """
    Enhanced multi-color sample detection pipeline
    Integrated from final pipeline implementations
    Returns: (largest_contour, processed_frame, llpython_data)
    """
    try:
        # Initialize return values
        llpython = [0, 0, 0, 0, 0, 0, 0, 0]  # [target_found, x, y, angle, contour_count, color_code, quadrant, area]
        largest_contour = np.array([[]])
        largest_area = 0

        frame_height, frame_width = frame.shape[:2]

        # Convert to HSV
        hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
        hsv_denoised = hsv  # Skip denoising for performance

        # Create masks for all colors
        blue_mask = cv2.inRange(hsv_denoised, np.array(HSV_BLUE_RANGE[0]), np.array(HSV_BLUE_RANGE[1]))
        blue_mask = cv2.erode(blue_mask, np.ones((3, 3), np.uint8))

        # Red mask (combine both ranges)
        red_mask1 = cv2.inRange(hsv_denoised, np.array(HSV_RED_RANGE_1[0]), np.array(HSV_RED_RANGE_1[1]))
        red_mask2 = cv2.inRange(hsv_denoised, np.array(HSV_RED_RANGE_2[0]), np.array(HSV_RED_RANGE_2[1]))
        red_mask = cv2.bitwise_or(red_mask1, red_mask2)
        red_mask = cv2.erode(red_mask, np.ones((3, 3), np.uint8))

        yellow_mask = cv2.inRange(hsv_denoised, np.array(HSV_YELLOW_RANGE[0]), np.array(HSV_YELLOW_RANGE[1]))
        yellow_mask = cv2.erode(yellow_mask, np.ones((3, 3), np.uint8))

        # Process each color
        all_samples = []

        # Process blue samples
        blue_contours, _, blue_gray, isDebug, _ = process_color(frame, blue_mask)
        if not isDebug:
            all_samples.extend(process_contours(blue_contours, blue_gray, "BLUE", frame_width, frame_height))

        # Process red samples
        red_contours, _, red_gray, isDebug, _ = process_color(frame, red_mask)
        if not isDebug:
            all_samples.extend(process_contours(red_contours, red_gray, "RED", frame_width, frame_height))

        # Process yellow samples
        yellow_contours, _, yellow_gray, isDebug, _ = process_color(frame, yellow_mask)
        if not isDebug:
            all_samples.extend(process_contours(yellow_contours, yellow_gray, "YELLOW", frame_width, frame_height))

        # Find the largest valid sample
        if all_samples:
            best_sample = max(all_samples, key=lambda x: x['area'])
            largest_contour = best_sample['contour']
            largest_area = best_sample['area']

            # Populate llpython array
            llpython = [
                1,                                      # [0] Target found
                best_sample['center'][0],               # [1] X coordinate
                best_sample['center'][1],               # [2] Y coordinate
                best_sample['angle'],                   # [3] Angle
                len(all_samples),                       # [4] Total contour count
                COLOR_CODES[best_sample['color']],      # [5] Color code
                best_sample['quadrant'],                # [6] Quadrant
                int(best_sample['area'])                # [7] Area
            ]

            # Draw all samples with color-coded visualization
            for sample in all_samples:
                color_bgr = (255, 0, 0) if sample['color'] == "BLUE" else (0, 0, 255) if sample['color'] == "RED" else (0, 255, 0)
                cv2.drawContours(frame, [sample['contour']], -1, color_bgr, 2)
                draw_info(frame, sample['color'], sample['angle'], sample['center'], sample['index'], sample['area'])

            # Add summary information
            cv2.putText(frame, f"Best: {best_sample['color']} | Samples: {len(all_samples)} | Area: {int(best_sample['area'])}",
                       (10, 20), cv2.FONT_HERSHEY_SIMPLEX, 0.4, (255, 255, 255), 1)
            cv2.putText(frame, f"Distance: {best_sample['distance']:.1f}in | Quadrant: {best_sample['quadrant']}",
                       (10, 35), cv2.FONT_HERSHEY_SIMPLEX, 0.4, (255, 255, 255), 1)

        return largest_contour, frame, llpython

    except Exception as e:
        # Error handling
        cv2.putText(frame, f"Error: {str(e)[:30]}", (10, 30),
                   cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)
        return np.array([[]]), frame, [0, 0, 0, 0, 0, 0, 0, 0]
