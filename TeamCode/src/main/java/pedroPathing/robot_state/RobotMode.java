package pedroPathing.robot_state;

public enum RobotMode {
    SAMPLE,
    SPECIMEN;

    // You could add methods specific to modes here
    public String getDescription() {
        switch(this) {
            case SAMPLE: return "Sample Collection Mode";
            case SPECIMEN: return "Specimen Handling Mode";
            default: return "Unknown Mode";
        }
    }
}
