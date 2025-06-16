"""
Enhanced Multi-Color Sample Detection Pipeline for Limelight 3A
Based on EXACT newest pipeline scripts: limelightblue_llm_debug_latest.py, 
limelightred_llm_debug_latest.py, limelightyellow_llm_debug_latest.py

This script combines all three working pipelines into a single multi-color detection system
using the EXACT same algorithms, parameters, and structure from the uploaded pipeline scripts.

Features:
- EXACT replication of working pipeline algorithms
- Multi-color detection (Blue, Red, Yellow) in single pipeline
- Identical edge detection and contour processing
- Same HSV ranges and filtering parameters
- Built-in debug capabilities from original scripts
- Color-specific visualization and output

Author: Based on EXACT newest FatDragon pipeline scripts
Version: 4.0 - Exact Multi-Color Pipeline Integration
"""

import cv2
import numpy as np
import math
import time
from collections import defaultdict

# ========== EXACT CONFIGURATION FROM NEWEST PIPELINE SCRIPTS ==========

# Constants for filtering contours - EXACT from newest scripts
SMALL_CONTOUR_AREA = 300

# Minimum average brightness threshold (0-255) - EXACT from newest scripts
MIN_BRIGHTNESS_THRESHOLD = 50

# HSV color ranges - EXACT from newest pipeline scripts
HSV_BLUE_RANGE = ([80, 60, 100], [110, 255, 255])           # From limelightblue_llm_debug_latest.py
HSV_RED_RANGE_1 = ([0, 60, 100], [10, 255, 255])           # From limelightred_llm_debug_latest.py
HSV_RED_RANGE_2 = ([170, 60, 100], [180, 255, 255])        # From limelightred_llm_debug_latest.py
HSV_YELLOW_RANGE = ([15, 60, 100], [80, 255, 255])         # From limelightyellow_llm_debug_latest.py

# Edge detection parameters - EXACT from newest scripts
BLUR_SIZE = 17
SOBEL_KERNEL = 3

# Aspect ratio range for contour filtering - EXACT from newest scripts
MIN_ASPECT_RATIO = 1.5  # Minimum width/height ratio
MAX_ASPECT_RATIO = 6.0  # Maximum width/height ratio

# Color codes for enhanced output
COLOR_CODES = {
    'BLUE': 1,
    'RED': 2,
    'YELLOW': 3,
    'UNKNOWN': 0
}

def calculate_angle(contour):
    """EXACT function from newest pipeline scripts"""
    if len(contour) < 5:
        return 0
    (x, y), (MA, ma), angle = cv2.fitEllipse(contour)
    return angle

def draw_info(image, color, angle, center, index, area):
    """Enhanced draw_info with color-specific visualization from newest scripts"""
    # Color-specific text colors - EXACT from newest scripts
    if color == "Blue":
        text_color = (255, 0, 0)  # Blue text for blue samples
    elif color == "Red":
        text_color = (0, 0, 255)  # Red text for red samples
    elif color == "Yellow":
        text_color = (0, 255, 0)  # Green text for yellow samples
    else:
        text_color = (255, 255, 255)  # White for unknown
    
    cv2.putText(image, f"#{index}: {color}", (center[0] - 40, center[1] - 60), 
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, text_color, 2)
    cv2.putText(image, f"Angle: {angle:.2f}", (center[0] - 40, center[1] - 40), 
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, text_color, 2)
    cv2.putText(image, f"Area: {area:.2f}", (center[0] - 40, center[1] - 20), 
                cv2.FONT_HERSHEY_SIMPLEX, 0.5, text_color, 2)
    cv2.circle(image, center, 5, text_color, -1)
    cv2.line(image, center, (int(center[0] + 50 * math.cos(math.radians(90 - angle))), 
                             int(center[1] - 50 * math.sin(math.radians(90 - angle)))), text_color, 2)

def separate_touching_contours(contour, min_area_ratio=0.15):
    """EXACT function from newest pipeline scripts"""
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
    """EXACT function from newest pipeline scripts"""
    return None, None, None, True, frame

def process_color(frame, mask):
    """EXACT process_color function from newest pipeline scripts"""
    debug_info = None
    #return pipeline_debug_return(frame)
    kernel = np.ones((5, 5), np.uint8)
    masked_frame = cv2.bitwise_and(frame, frame, mask=mask)  if 1 else frame
    #return pipeline_debug_return(masked_frame)
    gray_masked = cv2.cvtColor(masked_frame, cv2.COLOR_BGR2GRAY)  if 1 else masked_frame
    #return pipeline_debug_return(gray_masked)
    gray_boosted = cv2.addWeighted(gray_masked, 1.5, mask, 0.5, 0)  if 0 else gray_masked
    #return pipeline_debug_return(gray_boosted)
    blurred = cv2.GaussianBlur(gray_boosted, (3, 3), 0) if 1 else gray_boosted
    #return pipeline_debug_return(blurred)

    sobelx = cv2.Sobel(blurred, cv2.CV_32F, 1, 0, ksize=1)
    sobely = cv2.Sobel(blurred, cv2.CV_32F, 0, 1, ksize=1)

    magnitude = np.sqrt(sobelx**2 + sobely**2)
    magnitude = np.uint8(magnitude * 255 / np.max(magnitude))
    #return pipeline_debug_return(magnitude)

    _, edges = cv2.threshold(magnitude, 50, 255, cv2.THRESH_BINARY) if 1 else magnitude
    #return pipeline_debug_return(edges)

    edges = cv2.morphologyEx(edges, cv2.MORPH_CLOSE, kernel) if 1 else edges
    #return pipeline_debug_return(edges)
    edges = cv2.dilate(edges, np.ones((3, 3), np.uint8), iterations=1) if 1 else edges
    #return pipeline_debug_return(edges)
    edges = cv2.bitwise_not(edges) if 1 else edges
    #return pipeline_debug_return(edges)
    edges = cv2.bitwise_and(edges, edges, mask=mask) if 1 else edges
    #return pipeline_debug_return(edges)
    edges = cv2.GaussianBlur(edges, (3, 3), 0) if 1 else edges
    #return pipeline_debug_return(edges)
    contours, hierarchy = cv2.findContours(edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    return contours, hierarchy, gray_masked, False, debug_info

def debug_return(frame):
    """EXACT function from newest pipeline scripts"""
    return np.array([[]]), frame, [0, 0, 0, 0, 0, 0, 0, 0]

def process_single_color(frame, hsv_denoised, color_name, hsv_range_1, hsv_range_2=None):
    """Process a single color using EXACT logic from newest pipeline scripts"""
    valid_contours = []
    largest_area = 0
    largest_contour = np.array([[]])
    
    # Create mask - EXACT logic from newest scripts
    if hsv_range_2 is not None:  # Red color (two ranges)
        mask1 = cv2.inRange(hsv_denoised, np.array(hsv_range_1[0]), np.array(hsv_range_1[1]))
        mask2 = cv2.inRange(hsv_denoised, np.array(hsv_range_2[0]), np.array(hsv_range_2[1]))
        mask = cv2.bitwise_or(mask1, mask2)
    else:  # Blue or Yellow (single range)
        mask = cv2.inRange(hsv_denoised, np.array(hsv_range_1[0]), np.array(hsv_range_1[1]))
    
    mask = cv2.erode(mask, np.ones((3, 3), np.uint8))
    
    # Process color - EXACT from newest scripts
    contours, hierarchy, gray_masked, isDebug, debug_info = process_color(frame, mask)
    if isDebug:
        return valid_contours, largest_contour, largest_area
    
    # Process contours - EXACT logic from newest scripts
    for i, contour in enumerate(contours):
        if cv2.contourArea(contour) < SMALL_CONTOUR_AREA:
            continue

        # Check aspect ratio using minAreaRect - EXACT from newest scripts
        rect = cv2.minAreaRect(contour)
        width = max(rect[1])
        height = min(rect[1])
        if width == 0 or height == 0:
            continue
            
        aspect_ratio = width / height
        if aspect_ratio < MIN_ASPECT_RATIO or aspect_ratio > MAX_ASPECT_RATIO:
            continue

        for sep_contour in separate_touching_contours(contour):
            mask_check = np.zeros(gray_masked.shape, dtype=np.uint8)
            cv2.drawContours(mask_check, [sep_contour], -1, 255, -1)

            if cv2.mean(gray_masked, mask=mask_check)[0] < MIN_BRIGHTNESS_THRESHOLD:
                continue

            M = cv2.moments(sep_contour)
            if M["m00"] == 0:
                continue

            center = (int(M["m10"] / M["m00"]), int(M["m01"] / M["m00"]))
            angle = calculate_angle(sep_contour)
            area = cv2.contourArea(sep_contour)

            # Store valid contour info - EXACT from newest scripts
            valid_contours.append({
                'contour': sep_contour,
                'center': center,
                'angle': angle,
                'area': area,
                'index': i,
                'color': color_name,
                'contour_count': len(contours)
            })

            # Update largest_contour if this is the largest valid contour
            if area > largest_area:
                largest_area = area
                largest_contour = sep_contour
    
    return valid_contours, largest_contour, largest_area

def runPipeline(frame, llrobot):
    """
    Enhanced multi-color pipeline using EXACT logic from newest pipeline scripts
    Combines blue, red, and yellow detection in a single pipeline
    Returns: (largest_contour, processed_frame, llpython_data)
    """
    try:
        # Initialize Limelight-style output - EXACT from newest scripts
        llpython = [0, 0, 0, 0, 0, 0, 0, 0]
        largest_contour = np.array([[]])
        overall_largest_area = 0
        best_sample = None

        # Convert to HSV and denoise - EXACT from newest scripts
        hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
        hsv_denoised = cv2.GaussianBlur(hsv, (5, 5), 0)
        hsv_denoised = hsv  # Skip denoising like newest scripts

        all_valid_contours = []

        # Process Blue samples - EXACT from limelightblue_llm_debug_latest.py
        blue_contours, blue_largest, blue_area = process_single_color(
            frame, hsv_denoised, "Blue", HSV_BLUE_RANGE)
        all_valid_contours.extend(blue_contours)
        if blue_area > overall_largest_area:
            overall_largest_area = blue_area
            largest_contour = blue_largest
            if blue_contours:
                best_sample = max(blue_contours, key=lambda x: x['area'])

        # Process Red samples - EXACT from limelightred_llm_debug_latest.py
        red_contours, red_largest, red_area = process_single_color(
            frame, hsv_denoised, "Red", HSV_RED_RANGE_1, HSV_RED_RANGE_2)
        all_valid_contours.extend(red_contours)
        if red_area > overall_largest_area:
            overall_largest_area = red_area
            largest_contour = red_largest
            if red_contours:
                best_sample = max(red_contours, key=lambda x: x['area'])

        # Process Yellow samples - EXACT from limelightyellow_llm_debug_latest.py
        yellow_contours, yellow_largest, yellow_area = process_single_color(
            frame, hsv_denoised, "Yellow", HSV_YELLOW_RANGE)
        all_valid_contours.extend(yellow_contours)
        if yellow_area > overall_largest_area:
            overall_largest_area = yellow_area
            largest_contour = yellow_largest
            if yellow_contours:
                best_sample = max(yellow_contours, key=lambda x: x['area'])

        # Draw all valid contours and their info - EXACT style from newest scripts
        for contour_info in all_valid_contours:
            # Color-specific contour colors - EXACT from newest scripts
            if contour_info['color'] == "Blue":
                contour_color = (255, 0, 0)  # Blue contours
            elif contour_info['color'] == "Red":
                contour_color = (0, 0, 255)  # Red contours
            elif contour_info['color'] == "Yellow":
                contour_color = (0, 255, 0)  # Green contours for yellow
            else:
                contour_color = (255, 255, 255)  # White for unknown

            cv2.drawContours(frame, [contour_info['contour']], -1, contour_color, 2)
            draw_info(frame, contour_info['color'], contour_info['angle'],
                     contour_info['center'], contour_info['index'] + 1, contour_info['area'])

        # Populate llpython array - Enhanced with multi-color data
        if best_sample:
            llpython = [
                1,                                          # [0] Target found
                best_sample['center'][0],                   # [1] X coordinate
                best_sample['center'][1],                   # [2] Y coordinate
                best_sample['angle'],                       # [3] Angle
                best_sample['contour_count'],               # [4] Contour count for best color
                COLOR_CODES[best_sample['color'].upper()],  # [5] Color code
                len(all_valid_contours),                    # [6] Total samples detected
                int(best_sample['area'])                    # [7] Area
            ]

            # Add summary information - Enhanced multi-color display
            cv2.putText(frame, f"Best: {best_sample['color']} | Total: {len(all_valid_contours)} | Area: {int(best_sample['area'])}",
                       (10, 20), cv2.FONT_HERSHEY_SIMPLEX, 0.4, (255, 255, 255), 1)
            cv2.putText(frame, f"Blue: {len(blue_contours)} | Red: {len(red_contours)} | Yellow: {len(yellow_contours)}",
                       (10, 35), cv2.FONT_HERSHEY_SIMPLEX, 0.4, (255, 255, 255), 1)

        return largest_contour, frame, llpython

    except Exception as e:
        # Error handling - EXACT from newest scripts
        print(f"Error: {str(e)}")
        return np.array([[]]), frame, [0, 0, 0, 0, 0, 0, 0, 0]
