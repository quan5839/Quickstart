package pedroPathing.util;

/**
 * Custom PIDF Controller for precise motor control
 * Provides better control and debugging capabilities than FTC's built-in PIDF
 * 
 * @author Baron Henderson - 20077 The Indubitables
 * @version 1.0, 12/30/2024
 */
public class CustomPIDFController {
    
    // PIDF coefficients
    private double kP, kI, kD, kF;
    
    // Control variables
    private double targetPosition = 0;
    private double previousError = 0;
    private double integralSum = 0;
    private long lastUpdateTime = 0;
    
    // Integral windup prevention
    private double integralLimit = 1.0;
    private boolean enableIntegralLimit = true;
    
    // Output limits
    private double outputMin = -1.0;
    private double outputMax = 1.0;
    
    // Derivative filtering (to reduce noise)
    private double derivativeFilter = 0.1; // Low-pass filter coefficient (0-1, lower = more filtering)
    private double filteredDerivative = 0;
    
    // Tolerance for "at target" checking
    private double positionTolerance = 10.0; // encoder ticks
    private double velocityTolerance = 5.0; // ticks per second
    
    // Performance tracking
    private double lastPosition = 0;
    private double velocity = 0;
    
    /**
     * Constructor with PIDF coefficients
     * @param kP Proportional gain
     * @param kI Integral gain  
     * @param kD Derivative gain
     * @param kF Feed-forward gain
     */
    public CustomPIDFController(double kP, double kI, double kD, double kF) {
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
        this.kF = kF;
        this.lastUpdateTime = System.nanoTime();
    }
    
    /**
     * Calculate the control output for the given current position
     * @param currentPosition Current encoder position
     * @return Motor power output (-1.0 to 1.0)
     */
    public double calculate(double currentPosition) {
        long currentTime = System.nanoTime();
        double deltaTime = (currentTime - lastUpdateTime) / 1_000_000_000.0; // Convert to seconds
        
        // Avoid division by zero on first call
        if (lastUpdateTime == 0 || deltaTime <= 0) {
            lastUpdateTime = currentTime;
            lastPosition = currentPosition;
            return 0;
        }
        
        // Calculate error
        double error = targetPosition - currentPosition;
        
        // Calculate velocity (for derivative and feed-forward)
        velocity = (currentPosition - lastPosition) / deltaTime;
        
        // Proportional term
        double proportional = kP * error;
        
        // Integral term with windup prevention
        integralSum += error * deltaTime;
        if (enableIntegralLimit) {
            integralSum = Math.max(-integralLimit, Math.min(integralLimit, integralSum));
        }
        double integral = kI * integralSum;
        
        // Derivative term with filtering to reduce noise
        double rawDerivative = (error - previousError) / deltaTime;
        filteredDerivative = derivativeFilter * rawDerivative + (1 - derivativeFilter) * filteredDerivative;
        double derivative = kD * filteredDerivative;
        
        // Feed-forward term (helps with gravity compensation)
        double feedForward = kF * Math.signum(error);
        
        // Calculate total output
        double output = proportional + integral + derivative + feedForward;
        
        // Apply output limits
        output = Math.max(outputMin, Math.min(outputMax, output));
        
        // Update for next iteration
        previousError = error;
        lastUpdateTime = currentTime;
        lastPosition = currentPosition;
        
        return output;
    }
    
    /**
     * Set the target position
     * @param target Target position in encoder ticks
     */
    public void setTarget(double target) {
        this.targetPosition = target;
    }
    
    /**
     * Get the current target position
     * @return Target position in encoder ticks
     */
    public double getTarget() {
        return targetPosition;
    }
    
    /**
     * Update PIDF coefficients
     * @param kP Proportional gain
     * @param kI Integral gain
     * @param kD Derivative gain
     * @param kF Feed-forward gain
     */
    public void setPIDF(double kP, double kI, double kD, double kF) {
        this.kP = kP;
        this.kI = kI;
        this.kD = kD;
        this.kF = kF;
    }
    
    /**
     * Reset the controller state (useful when changing targets significantly)
     */
    public void reset() {
        previousError = 0;
        integralSum = 0;
        filteredDerivative = 0;
        lastUpdateTime = System.nanoTime();
    }
    
    /**
     * Check if the controller is at the target position
     * @param currentPosition Current encoder position
     * @return true if within tolerance
     */
    public boolean atTarget(double currentPosition) {
        double positionError = Math.abs(targetPosition - currentPosition);
        double velocityMagnitude = Math.abs(velocity);
        return positionError <= positionTolerance && velocityMagnitude <= velocityTolerance;
    }
    
    /**
     * Set position tolerance for "at target" checking
     * @param tolerance Tolerance in encoder ticks
     */
    public void setPositionTolerance(double tolerance) {
        this.positionTolerance = tolerance;
    }
    
    /**
     * Set velocity tolerance for "at target" checking
     * @param tolerance Tolerance in ticks per second
     */
    public void setVelocityTolerance(double tolerance) {
        this.velocityTolerance = tolerance;
    }
    
    /**
     * Set output limits
     * @param min Minimum output (-1.0 to 1.0)
     * @param max Maximum output (-1.0 to 1.0)
     */
    public void setOutputLimits(double min, double max) {
        this.outputMin = min;
        this.outputMax = max;
    }
    
    /**
     * Set integral limit for windup prevention
     * @param limit Maximum integral accumulation
     */
    public void setIntegralLimit(double limit) {
        this.integralLimit = limit;
        this.enableIntegralLimit = true;
    }
    
    /**
     * Disable integral windup prevention
     */
    public void disableIntegralLimit() {
        this.enableIntegralLimit = false;
    }
    
    /**
     * Get the current error
     * @param currentPosition Current encoder position
     * @return Position error in encoder ticks
     */
    public double getError(double currentPosition) {
        return targetPosition - currentPosition;
    }
    
    /**
     * Get the current velocity
     * @return Velocity in ticks per second
     */
    public double getVelocity() {
        return velocity;
    }
    
    /**
     * Get debug information as a formatted string
     * @param currentPosition Current encoder position
     * @return Debug string with all controller values
     */
    public String getDebugInfo(double currentPosition) {
        double error = getError(currentPosition);
        return String.format("Target: %.1f | Current: %.1f | Error: %.1f | Vel: %.1f | AtTarget: %s", 
                           targetPosition, currentPosition, error, velocity, atTarget(currentPosition));
    }
}
