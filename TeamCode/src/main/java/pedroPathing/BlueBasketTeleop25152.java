package pedroPathing;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import pedroPathing.robot_state.RobotMode;

/**
 * Blue team teleop for basket scoring mode.
 * Extends BaseTeleop25152 to eliminate code duplication.
 *
 * @author Baron Henderson - 20077 The Indubitables
 * @version 2.0, 12/30/2024
 */
@TeleOp(name = "Blue Basket Teleop")
public class BlueBasketTeleop25152 extends BaseTeleop25152 {

    @Override
    protected TeamColor getTeamColor() {
        return TeamColor.BLUE;
    }

    @Override
    protected RobotMode getRobotMode() {
        return RobotMode.SAMPLE; // Basket scoring uses SAMPLE mode
    }
}
