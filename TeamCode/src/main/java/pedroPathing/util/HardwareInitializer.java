package pedroPathing.util;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.RobotLog;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Advanced hardware initialization system with parallel processing and comprehensive error handling.
 * 
 * Features:
 * - Parallel hardware initialization for faster startup
 * - Comprehensive error handling and recovery
 * - Hardware health monitoring
 * - Graceful degradation when components fail
 * - Detailed initialization reporting
 * 
 * @author Baron Henderson - 20077 The Indubitables
 * @version 1.0, 12/30/2024
 */
public class HardwareInitializer {
    
    private final OpMode opMode;
    private final HardwareMap hardwareMap;
    private final Telemetry telemetry;
    
    // Initialization results
    private final List<InitResult> initResults = new ArrayList<>();
    private boolean bulkCachingEnabled = false;
    private int hubCount = 0;
    
    /**
     * Result of a hardware initialization attempt
     */
    public static class InitResult {
        public final String componentName;
        public final boolean success;
        public final String errorMessage;
        public final long initTimeMs;
        public final boolean critical; // If false, robot can operate without this component
        
        public InitResult(String componentName, boolean success, String errorMessage, long initTimeMs, boolean critical) {
            this.componentName = componentName;
            this.success = success;
            this.errorMessage = errorMessage;
            this.initTimeMs = initTimeMs;
            this.critical = critical;
        }
        
        public static InitResult success(String componentName, long initTimeMs, boolean critical) {
            return new InitResult(componentName, true, null, initTimeMs, critical);
        }
        
        public static InitResult failure(String componentName, String error, long initTimeMs, boolean critical) {
            return new InitResult(componentName, false, error, initTimeMs, critical);
        }
    }
    
    /**
     * Hardware component initializer interface
     */
    public interface ComponentInitializer {
        InitResult initialize() throws Exception;
    }
    
    public HardwareInitializer(OpMode opMode) {
        this.opMode = opMode;
        this.hardwareMap = opMode.hardwareMap;
        this.telemetry = opMode.telemetry;
    }
    
    /**
     * Initialize all hardware components with parallel processing
     * @return true if all critical components initialized successfully
     */
    public boolean initializeAllHardware() {
        long startTime = System.currentTimeMillis();
        telemetry.addData("Status", "Initializing hardware...");
        telemetry.update();
        
        // Clear previous results
        initResults.clear();
        
        // Step 1: Initialize REV Hubs and bulk caching (must be first)
        initializeBulkCaching();
        
        // Step 2: Create component initializers
        List<ComponentInitializer> initializers = createComponentInitializers();
        
        // Step 3: Run initializers in parallel
        ExecutorService executor = Executors.newCachedThreadPool();
        List<CompletableFuture<InitResult>> futures = new ArrayList<>();
        
        for (ComponentInitializer initializer : initializers) {
            CompletableFuture<InitResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return initializer.initialize();
                } catch (Exception e) {
                    return InitResult.failure("Unknown Component", e.getMessage(), 0, false);
                }
            }, executor);
            futures.add(future);
        }
        
        // Step 4: Wait for all initializations to complete
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(5, TimeUnit.SECONDS); // 5 second timeout
        } catch (Exception e) {
            RobotLog.ee("HardwareInitializer", e, "Timeout or error during parallel initialization");
        }
        
        // Step 5: Collect results
        for (CompletableFuture<InitResult> future : futures) {
            try {
                if (future.isDone()) {
                    initResults.add(future.get());
                }
            } catch (Exception e) {
                initResults.add(InitResult.failure("Future", e.getMessage(), 0, false));
            }
        }
        
        executor.shutdown();
        
        // Step 6: Analyze results and report
        long totalTime = System.currentTimeMillis() - startTime;
        return analyzeAndReportResults(totalTime);
    }
    
    /**
     * Initialize REV Hub bulk caching (must be done first)
     */
    private void initializeBulkCaching() {
        long startTime = System.currentTimeMillis();
        try {
            List<LynxModule> hubs = hardwareMap.getAll(LynxModule.class);
            hubCount = hubs.size();
            
            for (LynxModule hub : hubs) {
                hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
            }
            
            bulkCachingEnabled = true;
            long initTime = System.currentTimeMillis() - startTime;
            initResults.add(InitResult.success("REV Hubs (" + hubCount + ")", initTime, true));
            
        } catch (Exception e) {
            long initTime = System.currentTimeMillis() - startTime;
            initResults.add(InitResult.failure("REV Hubs", e.getMessage(), initTime, true));
            RobotLog.ee("HardwareInitializer", e, "Failed to initialize REV Hubs");
        }
    }
    
    /**
     * Create all component initializers
     * @return List of component initializers
     */
    private List<ComponentInitializer> createComponentInitializers() {
        List<ComponentInitializer> initializers = new ArrayList<>();
        
        // Drive system (critical)
        initializers.add(() -> {
            long start = System.currentTimeMillis();
            try {
                // Test that all drive motors are accessible
                hardwareMap.get("leftFront");
                hardwareMap.get("rightFront");
                hardwareMap.get("leftBack");
                hardwareMap.get("rightBack");
                return InitResult.success("Drive System", System.currentTimeMillis() - start, true);
            } catch (Exception e) {
                return InitResult.failure("Drive System", e.getMessage(), System.currentTimeMillis() - start, true);
            }
        });
        
        // IMU (critical)
        initializers.add(() -> {
            long start = System.currentTimeMillis();
            try {
                hardwareMap.get("imu");
                return InitResult.success("IMU", System.currentTimeMillis() - start, true);
            } catch (Exception e) {
                return InitResult.failure("IMU", e.getMessage(), System.currentTimeMillis() - start, true);
            }
        });
        
        // Intake system (critical)
        initializers.add(() -> {
            long start = System.currentTimeMillis();
            try {
                // Test key intake components
                hardwareMap.get("intakeSlideLeft");
                hardwareMap.get("intakeSlideRight");
                hardwareMap.get("intakeClaw");
                return InitResult.success("Intake System", System.currentTimeMillis() - start, true);
            } catch (Exception e) {
                return InitResult.failure("Intake System", e.getMessage(), System.currentTimeMillis() - start, true);
            }
        });
        
        // Outtake system (critical)
        initializers.add(() -> {
            long start = System.currentTimeMillis();
            try {
                // Test key outtake components
                hardwareMap.get("outtakeSlideLeft");
                hardwareMap.get("outtakeSlideRight");
                hardwareMap.get("outtakeClaw");
                return InitResult.success("Outtake System", System.currentTimeMillis() - start, true);
            } catch (Exception e) {
                return InitResult.failure("Outtake System", e.getMessage(), System.currentTimeMillis() - start, true);
            }
        });
        
        // Color sensors (non-critical)
        initializers.add(() -> {
            long start = System.currentTimeMillis();
            try {
                hardwareMap.get("intakeColorSensor");
                return InitResult.success("Intake Color Sensor", System.currentTimeMillis() - start, false);
            } catch (Exception e) {
                return InitResult.failure("Intake Color Sensor", e.getMessage(), System.currentTimeMillis() - start, false);
            }
        });
        
        // Vision system (non-critical)
        initializers.add(() -> {
            long start = System.currentTimeMillis();
            try {
                // Test webcam if available
                hardwareMap.get("Webcam 1");
                return InitResult.success("Vision System", System.currentTimeMillis() - start, false);
            } catch (Exception e) {
                return InitResult.failure("Vision System", e.getMessage(), System.currentTimeMillis() - start, false);
            }
        });

        // Limelight 3A (non-critical)
        initializers.add(() -> {
            long start = System.currentTimeMillis();
            try {
                hardwareMap.get(com.qualcomm.hardware.limelightvision.Limelight3A.class, "limelight");
                return InitResult.success("Limelight 3A", System.currentTimeMillis() - start, false);
            } catch (Exception e) {
                return InitResult.failure("Limelight 3A", e.getMessage(), System.currentTimeMillis() - start, false);
            }
        });

        return initializers;
    }
    
    /**
     * Analyze initialization results and provide comprehensive report
     * @param totalTimeMs Total initialization time
     * @return true if all critical components succeeded
     */
    private boolean analyzeAndReportResults(long totalTimeMs) {
        int successCount = 0;
        int criticalFailures = 0;
        int nonCriticalFailures = 0;
        long totalComponentTime = 0;
        
        // Analyze results
        for (InitResult result : initResults) {
            totalComponentTime += result.initTimeMs;
            
            if (result.success) {
                successCount++;
            } else {
                if (result.critical) {
                    criticalFailures++;
                } else {
                    nonCriticalFailures++;
                }
            }
        }
        
        // Generate report
        telemetry.addData("=== HARDWARE INIT REPORT ===", "");
        telemetry.addData("Total Time", String.format("%.1fs", totalTimeMs / 1000.0));
        telemetry.addData("Components", String.format("%d/%d successful", successCount, initResults.size()));
        telemetry.addData("REV Hubs", String.format("%d found, bulk caching %s", 
                hubCount, bulkCachingEnabled ? "ENABLED" : "FAILED"));
        
        if (criticalFailures > 0) {
            telemetry.addData("⚠️ CRITICAL FAILURES", criticalFailures);
        }
        
        if (nonCriticalFailures > 0) {
            telemetry.addData("⚠️ Non-Critical Failures", nonCriticalFailures);
        }
        
        // Detailed component status
        telemetry.addData("=== COMPONENT STATUS ===", "");
        for (InitResult result : initResults) {
            String status = result.success ? "✅" : (result.critical ? "❌" : "⚠️");
            String timeStr = String.format("%.0fms", (double) result.initTimeMs);
            
            if (result.success) {
                telemetry.addData(status + " " + result.componentName, timeStr);
            } else {
                telemetry.addData(status + " " + result.componentName, 
                        String.format("%s (%s)", timeStr, result.errorMessage));
            }
        }
        
        // Performance analysis
        double parallelEfficiency = totalComponentTime > 0 ? 
                (double) totalComponentTime / totalTimeMs : 1.0;
        telemetry.addData("Parallel Efficiency", String.format("%.1fx speedup", parallelEfficiency));
        
        telemetry.update();
        
        // Log detailed results
        RobotLog.dd("HardwareInitializer", "Initialization complete: %d/%d successful, %d critical failures", 
                successCount, initResults.size(), criticalFailures);
        
        return criticalFailures == 0;
    }
    
    /**
     * Get initialization results for external analysis
     * @return List of initialization results
     */
    public List<InitResult> getInitResults() {
        return new ArrayList<>(initResults);
    }
    
    /**
     * Check if a specific component initialized successfully
     * @param componentName Name of component to check
     * @return true if component initialized successfully
     */
    public boolean isComponentHealthy(String componentName) {
        return initResults.stream()
                .anyMatch(result -> result.componentName.equals(componentName) && result.success);
    }
    
    /**
     * Get total number of REV Hubs found
     * @return Hub count
     */
    public int getHubCount() {
        return hubCount;
    }
    
    /**
     * Check if bulk caching is enabled
     * @return true if bulk caching is working
     */
    public boolean isBulkCachingEnabled() {
        return bulkCachingEnabled;
    }
}
