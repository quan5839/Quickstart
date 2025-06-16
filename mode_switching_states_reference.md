# Robot State Machine - Mode Switching Reference

## 🟢 STATES THAT ALWAYS ALLOW MODE SWITCHING (25 states)

### General States (3)
- `INIT` - Initialization state, always safe to switch
- `HOLD` - Holding state, always safe to switch
- `TEST` - Test state, always safe to switch

### Sample Mode - Gamepad Input States (4)
- `SAMPLE_INTAKE_TARGET_CHECK` - Waits for right bumper press
- `SAMPLE_INTAKE_POSITION_CHECK` - Waits for right bumper or Y button
- `SAMPLE_INTAKE_CHECK_SAMPLE` - Waits for dpad down/right or right bumper
- `COMPLETE_INTAKE` - Waits for left bumper or right bumper

### Specimen Mode - Gamepad Input States (5)
- `SPECIMEN_INTAKE_TARGET_CHECK` - Waits for right bumper press
- `SPECIMEN_INTAKE_POSITION_CHECK` - Waits for right bumper or Y button
- `SPECIMEN_INTAKE_CHECK_SAMPLE` - Waits for dpad down/right or right bumper
- `SPECIMEN_INTAKE_READY` - Waits for right bumper press
- `SPECIMEN_INTAKE_RETRIEVE_SAMPLE` - Waits for right bumper press

### Undefined States (Likely Allow - 16)
- `SAMPLE_INTAKE_CLOSE`
- `SAMPLE_INTAKE_RELEASE_ELBOWS`
- `SAMPLE_INTAKE_CLAW_OPEN_WAIT`
- `SAMPLE_INTAKE_AVOID_CONFLICT`
- `SAMPLE_INTAKE_HOLD_OPTION`
- `SAMPLE_INTAKE_CANCEL`
- `SAMPLE_INTAKE_RESET_SLIDE`
- `SAMPLE_INTAKE_RELEASE_ELBOWS_DELAY`
- `SAMPLE_OUTTAKE_SLIDES_EXTEND`
- `SAMPLE_OUTTAKE_DUMP`
- `SAMPLE_OUTTAKE_COMPLETE_WAIT`
- `SAMPLE_REMOVE_FROM_INTAKE`
- `SAMPLE_REMOVE_FROM_OUTTAKE`
- `SPECIMEN_OUTTAKE_ELBOW_INTAKE`
- `SPECIMEN_OUTTAKE_SLIDE_LIFT`
- `SPECIMEN_OUTTAKE_PREPARE`
- `SPECIMEN_OUTTAKE_RELEASE`
- `SPECIMEN_INTAKE_POSITION_OUTTAKE`
- `SPECIMEN_INTAKE_CLOSE`
- `SPECIMEN_INTAKE_HOLD_OPTION`
- `SPECIMEN_INTAKE_CANCEL`
- `SPECIMEN_INTAKE_RETRACT_SLIDE`
- `SPECIMEN_INTAKE_EXTEND_SLIDE`
- `SPECIMEN_INTAKE_GRAB_CLOSE`
- `INTAKE_RELEASE_OPPONENT_SAMPLE`
- `INTAKE_REMOVE_RESET`

## 🔴 STATES THAT ALWAYS BLOCK MODE SWITCHING (15 states)

### Pure Timer States (9)
- `SAMPLE_INTAKE_GRAB_RESET` - Uses `IntakeConstants.CLAW_CLOSED_TIME`
- `SAMPLE_INTAKE_CLAW_CLOSE` - Uses `IntakeConstants.CLAW_CLOSED_TIME`
- `SAMPLE_INTAKE_SLIDES_CONTRACT` - Uses `OuttakeConstants.OUTTAKE_CLAW_CLOSED_TIME`
- `SAMPLE_INTAKE_CLAW_OPEN` - Uses `OuttakeConstants.OUTTAKE_CLAW_CLOSED_TIME`
- `SPECIMEN_OUTTAKE_SCORE` - Uses `OuttakeConstants.OUTTAKE_CLAW_CLOSED_TIME`
- `SPECIMEN_INTAKE_GRAB_RESET` - Uses `IntakeConstants.CLAW_CLOSED_TIME`
- `SPECIMEN_INTAKE_RESET_PICKED` - Uses `IntakeConstants.CLAW_CLOSED_TIME`
- `SPECIMEN_INTAKE_RELEASE_SAMPLE` - Uses `IntakeConstants.CLAW_CLOSED_TIME`
- `INTAKE_REMOVE_RESET_TURRET` - Uses `IntakeConstants.CLAW_CLOSED_TIME`

### Immediate Action States (6)
- `SAMPLE_INTAKE_TARGET` - Immediate hardware positioning
- `SAMPLE_INTAKE_GRAB` - Immediate claw/arm positioning
- `SAMPLE_INTAKE_WRIST_MOVE` - Immediate wrist movement
- `SPECIMEN_INTAKE_TARGET` - Immediate hardware positioning
- `SPECIMEN_INTAKE_GRAB` - Immediate claw/arm positioning
- `SPECIMEN_INTAKE_CLAW_CLOSE` - Immediate claw/arm positioning
- `INTAKE_REMOVE_OPPONENT_SAMPLE` - Immediate removal positioning

## 🟡 STATES WITH CONDITIONAL MODE SWITCHING (3 states)

### Block During Timer, Allow After Timer Completes
- `SAMPLE_OUTTAKE_ELBOW_BASKET` - 500ms timer + left bumper condition
- `SPECIMEN_OUTTAKE_CLAW_CLOSE` - `IntakeConstants.CLAW_CLOSED_TIME` + left bumper condition  
- `SPECIMEN_OUTTAKE_CHECK` - 400ms timer + left bumper condition

## Implementation Details

### Sample Color Re-checking
Before switching modes, the system re-checks BOTH sample colors:
- **Intake Sample**: `getDirectSampleColor()` - Updates `intakeSample` if detected
- **Outtake Sample**: `getDirectOuttakeSampleColor()` - Updates `outtakeSample` if detected
- Logs both detections separately for debugging

### Enhanced Telemetry
The system now provides detailed feedback when mode switching is blocked:
- "Outtake reset in progress" - When outtake reset is ongoing
- "Timer-based state: [STATE_NAME] ([TIME]ms remaining)" - During active timers
- "Timer-based state: [STATE_NAME] (pure timer)" - For pure timer states
- "Timer-based state: [STATE_NAME] (immediate action)" - For immediate action states
- "Timer-based state: End delay active" - During end delays

### Smart Logic
- **Pure timer states**: Always blocked (no gamepad input available)
- **Immediate action states**: Always blocked (executing hardware actions immediately)
- **Timer + gamepad states**: Blocked only while timer is running
- **Gamepad-only states**: Always allow mode switching
- **End delay states**: Always blocked (most sensitive timing)
