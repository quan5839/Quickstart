# ROBOT MAP - 2024-2025 SEASON

> **Update this file every time you rewire or reprogram hardware!**

## Motors

| Name in Code        | Hub/Port               |
|---------------------|------------------------|
| `rightFront`        | Control Hub / Port 0   |
| `rightBack`         | Control Hub / Port 1   |
| `outtakeSlideRight` | Control Hub / Port 2   |
| `leftFront`         | Expansion Hub / Port 0 |
| `leftBack`          | Expansion Hub / Port 1 |
| `outtakeSlideLeft`  | Expansion Hub / Port 2 |

## Servos

| Name in Code         | Hub/Port              | In-App Config Name   | Function/Location                |
|----------------------|-----------------------|----------------------|----------------------------------|
| `intakeClaw`         | Control Hub / Port 0  | intakeClaw           | Grabs intake sample              |
| `intakeWrist`        | Control Hub / Port 1  | intakeWrist          | Rotates intake                   |
| `intakeWrist`        | Control Hub / Port 1  | intakeWrist          | Rotates intake                   |

| `intakeElbowLeft`    | Exp. Hub / Port 0     | intakeElbowLeft      | Left intake elbow joint          |
| `intakeElbowRight`   | Exp. Hub / Port 1     | intakeElbowRight     | Right intake elbow joint         |

## Sensors

| Name in Code         | Hub/Port              | In-App Config Name   | Function/Location                |
|----------------------|-----------------------|----------------------|----------------------------------|
| `imu`                | Built-in              | imu                  | Robot orientation (Rev Hub IMU)  |
| ...                  | ...                   | ...                  | ...                              |

## Notes

- Update this file anytime you add, remove, or rename a device OR move a cable!
- Keep port numbers and names in code/config 100% in sync.
- Comment what each part does. Update as your robot changes during the season!
- Refer to this + the config xml for every pit debug and new programmer onboarding.

---

*Legend:*

- **Hub/Port** refers to hardware device port (zero indexed for SDK, 1 indexed for Rev UI).
- **In-App Config Name** matches _exactly_ what you assign in the configuration UI.
- **Name in Code** is the key for `hardwareMap.get()` calls.

---

<?xml version='1.0' encoding='UTF-8' standalone='yes' ?> <!--This is the xml declaration and can be copy-pasted-->
<!--Author: Havish Sripada - 12808 RevAmped Robotics-->

<!--This declares the robot class, and can be copy-pasted.-->
<Robot type="FirstInspires-FTC">

    <!--This line declares the Control Hub Portal which contains both hubs. It can be copy-pasted-->
    <LynxUsbDevice name="Control Hub Portal" serialNumber="(embedded)" parentModuleAddress="173">

        <!--This line declares the Expansion Hub. We use RS485 connection, so we declare it with a port.-->
        <LynxModule name="Expansion Hub 2" port="2">

            <!--These are the Expansion Hub's motors. Change the ports and names of your motors.-->
            <goBILDA5203SeriesMotor name="leftFront" port="0"/>
            <goBILDA5203SeriesMotor name="leftBack" port="1"/>
            <goBILDA5203SeriesMotor name="outtakeSlideLeft" port="2"/>

            <!--These are the Expansion Hub's servos. Change the ports and names of your servos.-->
            <Servo name="intakeElbowLeft" port="0"/>
            <Servo name="intakeSlideLeft" port="1"/>
            <Servo name="outtakeElbowRight" port="2"/>
            <Servo name="outtakeElbowLeft" port="3"/>
            <Servo name="outtakeWrist" port="4"/>
            <Servo name="outtakeClaw" port="5"/>

            <AnalogInputDevice name="leftUS" port="0"/>


            <!--This is a REV 2m Distance Sensor. Since this is an I^2C device, we require the type of device in the configuration as well.-->
            <REV_VL53L0X_RANGE_SENSOR name="laser" port="0" bus="2" />

            <!--This is a limit switch. Since we plugged it into digital devices, we don't need to specify this in our config.-->
<!--            <DigitalDevice name="vswitch" port="0"/>-->

            <!--This line signifies that the Expansion Hub configuration has ended.-->
        </LynxModule>

        <!--This line declares the Control Hub and can be copy-pasted.-->
        <LynxModule name="Control Hub" port="173">

            <!--These are the Control Hub's motors. Change the ports and names of your motors.-->
            <goBILDA5202SeriesMotor name="rightFront" port="0" />
            <goBILDA5202SeriesMotor name="rightBack" port="1" />
            <goBILDA5202SeriesMotor name="outtakeSlideRight" port="2" />
<!--            <goBILDA5202SeriesMotor name="motor_rb" port="3" />-->

            <!--This is a limit switch. Since we plugged it into digital devices, we don't need to specify this in our config.-->
<!--            <DigitalDevice name="hSwitch" port="0"/>-->

            <!--This is a REV Color Sensor V3. Since this is an I^2C device, we require the type of device in the configuration as well.-->
<!--            <RevColorSensorV3 name="colorSensor" port="0" bus="2"/>-->

            <!--This is a GoBilda Pinpoint Computer. Use this to configure your pinpoint if you are using one of the pinpoint localizers.-->
<!--            <goBILDAPinpoint name="pinpoint" port="0" bus="3"/>-->

            <!--These are the Control Hub's servos. Change the ports and names of your servos.-->
            <Servo name="intakeClaw" port="0"/>
            <Servo name="intakeWrist" port="1"/>
            <Servo name="intakeElbowRight" port="2"/>
            <Servo name="intakeSlideRight" port="3"/>

            <AnalogInputDevice name="rightUS" port="0"/>

            <!--This is the built-in Control Hub IMU. Use this if you are using one of the Control Hub IMU localizers.-->
            <ControlHubImuBHI260AP name="imu" port="0" bus="0"/>

            <HuskyLens name="Husky" port="0" bus="1"/>

            <!--This line signifies that the Control Hub configuration has ended.-->
        </LynxModule>

        <!--This line signifies that the Control Hub Portal configuration has ended.-->
    </LynxUsbDevice>

    <!--This is the configuration for an Ethernet Device, in this case our Limelight3A.-->
    <EthernetDevice serialNumber="EthernetOverUsb:eth0:172.29.0.25" name="webcam" port="0" ipAddress="172.29.0.1" />

    <!--This line signifies that the robot configuration has ended-->
    <Calibrations>

        <!-- ======================================================================================= -->

        <!-- Microsoft Lifecam HD 3000 v1, Calibrated by PTC using unknown tooling -->
        <!-- <Camera vid="Microsoft" pid="0x0779">
            <Calibration
                size="640 480"
                focalLength="678.154f, 678.17f"
                principalPoint="318.135f, 228.374f"
                distortionCoefficients="0.154576f, -1.19143f, 0f, 0f, 2.06105f, 0f, 0f, 0f"
                />
        </Camera> -->

        <!-- ======================================================================================= -->

        <!-- Microsoft Lifecam HD 3000 v2, Calibrated by PTC using unknown tooling -->
        <!-- <Camera vid="Microsoft" pid="0x0810">
            <Calibration
                size="640 480"
                focalLength="678.154f, 678.17f"
                principalPoint="318.135f, 228.374f"
                distortionCoefficients="0.154576f, -1.19143f, 0f, 0f, 2.06105f, 0f, 0f, 0f"
                />
        </Camera> -->

        <!-- ======================================================================================= -->

        <!-- Logitech HD Webcam C310, Calibrated by by Robert Atkinson, 2018.05.30 using 3DF Zephyr -->
        <!-- <Camera vid="Logitech" pid="0x081B">
            <Calibration
                size="640 480"
                focalLength="821.993f, 821.993f"
                principalPoint="330.489f, 248.997f"
                distortionCoefficients="-0.018522, 1.03979, 0, 0, -3.3171, 0, 0, 0"
                />

            <Calibration
                size="640 360"
                focalLength="715.307f, 715.307f"
                principalPoint="319.759f, 188.917f"
                distortionCoefficients="-0.0258948, 1.06258, 0, 0, -3.40245, 0, 0, 0"
                />
        </Camera> -->

        <!-- ======================================================================================= -->

        <!-- Logitech HD Pro Webcam C920, Calibrated by Robert Atkinson, 2018.05.30 using 3DF Zephyr -->
        <!-- <Camera vid="Logitech" pid="0x082D">

            <Calibration
                size="640 480"
                focalLength="622.001f, 622.001f"
                principalPoint="319.803f, 241.251f"
                distortionCoefficients="0.1208, -0.261599, 0, 0, 0.10308, 0, 0, 0"
                />

            <Calibration
                size="800 600"
                focalLength="775.79f, 775.79f"
                principalPoint="400.898f, 300.79f"
                distortionCoefficients="0.112507, -0.272067, 0, 0, 0.15775, 0, 0, 0"
                />

            <Calibration
                size="640 360"
                focalLength="463.566f, 463.566f"
                principalPoint="316.402f, 176.412f"
                distortionCoefficients="0.111626 , -0.255626, 0, 0, 0.107992, 0, 0, 0"
                />

            <Calibration
                size="1920, 1080"
                focalLength="1385.92f , 1385.92f"
                principalPoint="951.982f , 534.084f"
                distortionCoefficients="0.117627, -0.248549, 0, 0, 0.107441, 0, 0, 0"
                />

            <Calibration
                size="800, 448"
                focalLength="578.272f , 578.272f"
                principalPoint="402.145f , 221.506f"
                distortionCoefficients="0.12175, -0.251652 , 0, 0, 0.112142, 0, 0, 0"
                />

            <Calibration
                size="864, 480"
                focalLength="626.909f , 626.909f"
                principalPoint="426.007f , 236.834f"
                distortionCoefficients="0.120988, -0.253336 , 0, 0, 0.102445, 0, 0, 0"
                />

        </Camera> -->

        <!-- ======================================================================================= -->

        <!-- Logitech HD Webcam C270, Calibrated by Noah Andrews, 2019.03.13 using 3DF Zephyr -->
        <!--<Camera vid="Logitech" pid="0x0825">
            <Calibration
                size="640 480"
                focalLength="822.317f, 822.317f"
                principalPoint="319.495f, 242.502f"
                distortionCoefficients="-0.0449369, 1.17277, 0, 0, -3.63244, 0, 0, 0"
                />
        </Camera> -->

        <!-- ======================================================================================= -->

    </Calibrations>

</Robot>

<!--
  This file can provide additional camera calibration settings beyond those built into the SDK itself.
  Each calibration is for a particular camera (indicated by USB vid & pid pair) and a particular
  capture resolution for the camera. Note: it is very important when capturing images used to calibrate
  a camera that the image acquisition tool can actually control this capture resolution within the camera
  itself and that you use this setting correctly. Many image acquistion tools do not in fact provide
  this level of control.

  Beyond simply providing additional, new camera calibrations, calibrations provided herein can
  *replace/update* those that are builtin to the SDK. This matching is keyed, of course, by the
  (vid, pid, size) triple. Further, if such a calibration has the 'remove' attribute with value 'true',
  any existing calibration with that key is removed (and the calibration itself not added).

  Calibrations are internally processed according to aspect ratio. If a format is requested in a size
  that is not calibrated, but a calibration does exist for the same aspect ratio on the same camera,
  then the latter will be scaled to accommodate the request. For example, if a 640x480 calibration
  is requested but only a 800x600 calibration exists for that camera, then the 800x600 is scaled
  down to service the 640x480 request.

  Further, it is important to note that if *no* calibrations exist for a given camera, then Vuforia
  is offered the entire range of capture resolutions that the hardware can support (and it does its
  best to deal with the lack of calibration). However, if *any* calibrations are provided for a camera,
  then capture resolutions in those aspect ratios supported by the camera for which any calibrations
  are *not* provided are *not* offered. Thus, if you calibrate a camera but fail to calibrate all
  the camera's supported aspect ratios, you limit the choices of capture resolutions that Vuforia can
  select from.

  One image acquisition program that supports control of camera capture resolution is YouCam 7:
    https://www.cyberlink.com/products/youcam/features_en_US.html

  Programs that can process acquired images to determine camera calibration settings include:
    https://www.3dflow.net/3df-zephyr-free/ (see "Utilities/Images/Launch Camera Calibration" therein)
    http://graphics.cs.msu.ru/en/node/909
  Note that the type of images that must be acquired in order to calibrate is specific to the
  calibration software used.

  The required contents are illustrated here by example. Note that for the attribute names, both the
  camelCase or the underscore_variations are supported; they are equivalent. The attributes for
  each Calibration are as follows:

    size (int pair): space separated camera resolution (width, height).
    focalLength (float pair): space separated focal length value.
    principalPoint (float pair): space separated principal point values (width, height).
    distortionCoefficients (an 8-element float array): distortion coefficients in the following form
        (r:radial, t:tangential): [r0, r1, t0, t1, r2, r3, r4, r5]
        see https://docs.opencv.org/2.4/modules/calib3d/doc/camera_calibration_and_3d_reconstruction.html

  The examples here are commented out as the values are built-in to the FTC SDK. They serve instead
  here as examples on how make your own.

-->
