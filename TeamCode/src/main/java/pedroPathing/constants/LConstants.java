package pedroPathing.constants;

import com.pedropathing.localization.GoBildaPinpointDriver;
import com.pedropathing.localization.constants.PinpointConstants;

import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit;

public class LConstants {

    // Drive Encoder
//    static {
//        DriveEncoderConstants.forwardTicksToInches = 0.006;
//        DriveEncoderConstants.strafeTicksToInches = 0.006;
//        DriveEncoderConstants.turnTicksToInches = 0.01405;
//
//        DriveEncoderConstants.robot_Width = 17.72;
//        DriveEncoderConstants.robot_Length = 17.72;
//
//        DriveEncoderConstants.leftFrontEncoderDirection = Encoder.REVERSE;
//        DriveEncoderConstants.leftRearEncoderDirection = Encoder.REVERSE;
//        DriveEncoderConstants.rightFrontEncoderDirection = Encoder.FORWARD;
//        DriveEncoderConstants.rightRearEncoderDirection = Encoder.FORWARD;
//    }

//     Pinpoint Encoder
    static {
        PinpointConstants.forwardY = -1.38;
        PinpointConstants.strafeX = 0;
        PinpointConstants.distanceUnit = DistanceUnit.INCH;
        PinpointConstants.hardwareMapName = "pinpoint";
        PinpointConstants.useYawScalar = false;
        PinpointConstants.yawScalar = 1.0;
        PinpointConstants.useCustomEncoderResolution = false;
        PinpointConstants.encoderResolution = GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD;
        PinpointConstants.customEncoderResolution = 13.26291192;
        PinpointConstants.forwardEncoderDirection = GoBildaPinpointDriver.EncoderDirection.FORWARD;
        PinpointConstants.strafeEncoderDirection = GoBildaPinpointDriver.EncoderDirection.REVERSED;
    }
}




