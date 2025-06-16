package pedroPathing.util;

import java.util.function.BooleanSupplier;

import pedroPathing.robot_state.RobotState;

/**
 * Builder pattern utility for creating state machine transitions
 * Improves readability and reduces code duplication in state machine setup
 * 
 * @author Baron Henderson - 20077 The Indubitables
 * @version 1.0, 12/30/2024
 */
public class StateTransitionBuilder {
    
    /**
     * Lightweight StateTransition class for improved performance
     */
    public static class StateTransition {
        public final RobotState currentState;
        public final double waitTimeMs;
        public final BooleanSupplier condition;
        public final Runnable action;
        public final RobotState nextState;
        public final double endDelayMs;

        public StateTransition(RobotState currentState, double waitTimeMs, BooleanSupplier condition, Runnable action, RobotState nextState, double endDelayMs) {
            this.currentState = currentState;
            this.waitTimeMs = waitTimeMs;
            this.condition = condition;
            this.action = action;
            this.nextState = nextState;
            this.endDelayMs = endDelayMs;
        }

        // Backward compatibility constructor
        public StateTransition(RobotState currentState, double waitTimeMs, BooleanSupplier condition, Runnable action, RobotState nextState) {
            this(currentState, waitTimeMs, condition, action, nextState, 0);
        }
    }
    
    private RobotState currentState;
    private double waitTimeMs = 0;
    private BooleanSupplier condition = null;
    private Runnable action = null;
    private RobotState nextState;
    private double endDelayMs = 0;
    
    /**
     * Start building a transition from the specified state
     */
    public static StateTransitionBuilder from(RobotState state) {
        StateTransitionBuilder builder = new StateTransitionBuilder();
        builder.currentState = state;
        return builder;
    }
    
    /**
     * Set the wait time before this transition can occur
     */
    public StateTransitionBuilder waitMs(double timeMs) {
        this.waitTimeMs = timeMs;
        return this;
    }
    
    /**
     * Set the condition that must be true for this transition
     */
    public StateTransitionBuilder when(BooleanSupplier condition) {
        this.condition = condition;
        return this;
    }
    
    /**
     * Set the action to execute when transitioning
     */
    public StateTransitionBuilder execute(Runnable action) {
        this.action = action;
        return this;
    }
    
    /**
     * Set the target state for this transition
     */
    public StateTransitionBuilder goTo(RobotState nextState) {
        this.nextState = nextState;
        return this;
    }

    /**
     * Set the delay time after condition is met before transitioning
     */
    public StateTransitionBuilder endDelayMs(double timeMs) {
        this.endDelayMs = timeMs;
        return this;
    }

    /**
     * Build the final StateTransition object
     */
    public StateTransition build() {
        if (currentState == null || nextState == null) {
            throw new IllegalStateException("Current state and next state must be specified");
        }
        return new StateTransition(currentState, waitTimeMs, condition, action, nextState, endDelayMs);
    }
    
    /**
     * Convenience method for immediate transitions (no wait, no condition)
     */
    public static StateTransition immediate(RobotState from, Runnable action, RobotState to) {
        return new StateTransition(from, 0, null, action, to, 0);
    }

    /**
     * Convenience method for timed transitions (wait time, no condition)
     */
    public static StateTransition timed(RobotState from, double waitMs, Runnable action, RobotState to) {
        return new StateTransition(from, waitMs, null, action, to, 0);
    }

    /**
     * Convenience method for conditional transitions (condition, no wait)
     */
    public static StateTransition conditional(RobotState from, BooleanSupplier condition, Runnable action, RobotState to) {
        return new StateTransition(from, 0, condition, action, to, 0);
    }

    /**
     * Convenience method for conditional transitions with end delay
     */
    public static StateTransition conditionalWithEndDelay(RobotState from, BooleanSupplier condition, Runnable action, RobotState to, double endDelayMs) {
        return new StateTransition(from, 0, condition, action, to, endDelayMs);
    }

    /**
     * Convenience method for simple state changes (no action, no wait, no condition)
     */
    public static StateTransition simple(RobotState from, RobotState to) {
        return new StateTransition(from, 0, null, null, to, 0);
    }
}
