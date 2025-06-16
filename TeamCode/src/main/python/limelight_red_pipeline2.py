import cv2
import numpy as np
import math

# Constants for red detection - based on FTC-12527-FatDragon implementation
# Red requires dual HSV ranges due to hue wrap-around at 0/180
HSV_RED_RANGE_1 = ([0, 120, 70], [10, 255, 255])
HSV_RED_RANGE_2 = ([170, 120, 70], [180, 255, 255])
SMALL_CONTOUR_AREA = 300
MIN_BRIGHTNESS_THRESHOLD = 50
MIN_ASPECT_RATIO = 1.5
MAX_ASPECT_RATIO = 6.0

def calculate_angle(contour):
    """Calculate the angle of the contour using fitted ellipse"""
    if len(contour) < 5:
        return 0
    (x, y), (MA, ma), angle = cv2.fitEllipse(contour)
    return angle

def separate_touching_contours(contour, min_area_ratio=0.15):
    """Separate touching contours using distance transform - FatDragon method"""
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

def process_color(frame, mask):
    """Process color mask using edge detection - FatDragon method"""
    kernel = np.ones((5, 5), np.uint8)
    masked_frame = cv2.bitwise_and(frame, frame, mask=mask)
    
    gray_masked = cv2.cvtColor(masked_frame, cv2.COLOR_BGR2GRAY)
    blurred = cv2.GaussianBlur(gray_masked, (3, 3), 0)
    
    # Sobel edge detection
    sobelx = cv2.Sobel(blurred, cv2.CV_32F, 1, 0, ksize=1)
    sobely = cv2.Sobel(blurred, cv2.CV_32F, 0, 1, ksize=1)
    magnitude = np.sqrt(sobelx**2 + sobely**2)
    magnitude = np.uint8(magnitude * 255 / np.max(magnitude))
    
    _, edges = cv2.threshold(magnitude, 50, 255, cv2.THRESH_BINARY)
    edges = cv2.morphologyEx(edges, cv2.MORPH_CLOSE, kernel)
    edges = cv2.dilate(edges, np.ones((3, 3), np.uint8), iterations=1)
    edges = cv2.bitwise_not(edges)
    edges = cv2.bitwise_and(edges, edges, mask=mask)
    edges = cv2.GaussianBlur(edges, (3, 3), 0)
    
    contours, hierarchy = cv2.findContours(edges, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
    return contours, hierarchy, gray_masked

def runPipeline(frame, llrobot):
    """
    Main pipeline function for red sample detection
    
    Returns:
    - largest_contour: Largest detected contour for Limelight crosshairs
    - frame: Processed frame with detection overlays
    - llpython: 8-element array with detection data
      [0] = samples detected (0 or 1)
      [1] = X position (pixels)
      [2] = Y position (pixels)  
      [3] = angle (degrees)
      [4] = color code (1 = red)
      [5] = quadrant (0 = none, 1 = top, 2 = bottom)
      [6] = confidence (0-100)
      [7] = area (pixels)
    """
    try:
        # Initialize Limelight-style output
        llpython = [0, 0, 0, 0, 0, 0, 0, 0]
        largest_contour = np.array([[]])
        largest_area = 0
        
        # Convert to HSV and denoise
        hsv = cv2.cvtColor(frame, cv2.COLOR_BGR2HSV)
        hsv_denoised = cv2.GaussianBlur(hsv, (5, 5), 0)
        
        # Create mask for red (combining both ranges due to hue wrap-around)
        red_mask1 = cv2.inRange(hsv_denoised, np.array(HSV_RED_RANGE_1[0]), np.array(HSV_RED_RANGE_1[1]))
        red_mask2 = cv2.inRange(hsv_denoised, np.array(HSV_RED_RANGE_2[0]), np.array(HSV_RED_RANGE_2[1]))
        red_mask = cv2.bitwise_or(red_mask1, red_mask2)
        red_mask = cv2.erode(red_mask, np.ones((3, 3), np.uint8))
        
        # Process red color using FatDragon method
        red_contours, red_hierarchy, red_gray = process_color(frame, red_mask)
        
        valid_contours = []
        
        for i, contour in enumerate(red_contours):
            if cv2.contourArea(contour) < SMALL_CONTOUR_AREA:
                continue
            
            # Check aspect ratio using minAreaRect
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
                mask = np.zeros(red_gray.shape, dtype=np.uint8)
                cv2.drawContours(mask, [sep_contour], -1, 255, -1)
                
                if cv2.mean(red_gray, mask=mask)[0] < MIN_BRIGHTNESS_THRESHOLD:
                    continue
                
                # Calculate center and angle
                M = cv2.moments(sep_contour)
                if M["m00"] == 0:
                    continue
                
                center = (int(M["m10"] / M["m00"]), int(M["m01"] / M["m00"]))
                angle = calculate_angle(sep_contour)
                area = cv2.contourArea(sep_contour)
                
                # Determine quadrant (top/bottom half of frame)
                frame_height = frame.shape[0]
                quadrant = 1 if center[1] < frame_height / 2 else 2
                
                # Store valid contour info
                valid_contours.append({
                    'contour': sep_contour,
                    'center': center,
                    'angle': angle,
                    'area': area,
                    'quadrant': quadrant
                })
                
                # Update llpython and largest_contour if this is the largest valid contour
                if area > largest_area:
                    largest_area = area
                    largest_contour = sep_contour
                    llpython = [1, center[0], center[1], angle, 1, quadrant, 100, area]
        
        # Draw all valid contours and their info
        for contour_info in valid_contours:
            # Draw contour in red
            cv2.drawContours(frame, [contour_info['contour']], -1, (0, 0, 255), 2)
            
            # Draw center point
            cv2.circle(frame, contour_info['center'], 5, (0, 0, 255), -1)
            
            # Draw angle line
            center = contour_info['center']
            angle = contour_info['angle']
            end_point = (
                int(center[0] + 50 * math.cos(math.radians(90 - angle))),
                int(center[1] - 50 * math.sin(math.radians(90 - angle)))
            )
            cv2.line(frame, center, end_point, (0, 0, 255), 2)
            
            # Draw info text
            cv2.putText(frame, f"Red", 
                       (center[0] - 40, center[1] - 60),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)
            cv2.putText(frame, f"A: {contour_info['area']:.0f}", 
                       (center[0] - 40, center[1] - 40),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)
            cv2.putText(frame, f"Q: {contour_info['quadrant']}", 
                       (center[0] - 40, center[1] - 20),
                       cv2.FONT_HERSHEY_SIMPLEX, 0.5, (0, 0, 255), 2)
        
        return largest_contour, frame, llpython
        
    except Exception as e:
        print(f"Red pipeline error: {str(e)}")
        return np.array([[]]), frame, [0, 0, 0, 0, 0, 0, 0, 0]
