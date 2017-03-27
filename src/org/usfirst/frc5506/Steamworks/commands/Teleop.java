// RobotBuilder Version: 2.0
//
// This file was generated by RobotBuilder. It contains sections of
// code that are automatically generated and assigned by robotbuilder.
// These sections will be updated in the future when you export to
// Java from RobotBuilder. Do not put any code or make any change in
// the blocks indicating autogenerated code or it will be lost on an
// update. Deleting the comments indicating the section will prevent
// it from being updated in the future.

package org.usfirst.frc5506.Steamworks.commands;

import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import org.usfirst.frc5506.Steamworks.Robot;

/**
 * Current mappings:
 * 
 * Sticks	Drive
 * LB		Toggle speed
 * A		Kill routines
 * B		Start climbing
 * X		Stop climbing
 * B + X	Rewind climber
 * Y		Auto gear
 */
public class Teleop extends Command {
	// "true" removes tank drive functionality, and switches to arcade drive via j1
	public boolean j1arcade = false;

	// was "LB" pressed in the previous tick?
	public boolean lbWasPressed = false;

	public boolean fullPower = false;

	public Teleop() {
		requires(Robot.driveTrain);
	}

	// Called repeatedly when this Command is scheduled to run
	protected void execute() {
		// speed toggle
		if (!lbWasPressed && Robot.oi.getDriverJoystick().getRawButton(5)) { // LB
			lbWasPressed = true;
			fullPower = !fullPower;
		} else if (lbWasPressed && !Robot.oi.getDriverJoystick().getRawButton(5)) {
			lbWasPressed = false;
		}
		SmartDashboard.putBoolean("Power", fullPower);

		if (Robot.oi.getDriverJoystick().getRawButton(2) && // B
			Robot.oi.getDriverJoystick().getRawButton(3)) // X
			Robot.climber.set(-0.2);
		else if (Robot.oi.getDriverJoystick().getRawButton(2)) // B
			Robot.climber.set(1);
		else if (Robot.oi.getDriverJoystick().getRawButton(3)) // X
			Robot.climber.set(0);
		if (j1arcade) {
			double x = Robot.oi.getFunctionJoystick().getX() / (fullPower ? 1 : 2);
			double y = -Robot.oi.getFunctionJoystick().getY() / (fullPower ? 1 : 2);
			Robot.driveTrain.driveArcade(y, x);
		} else {
			// left axis = 1, right axis = 5
			double leftSpeed = -Robot.oi.getDriverJoystick().getRawAxis(1);
			double rightSpeed = -Robot.oi.getDriverJoystick().getRawAxis(5);
			if (Robot.driveTrain.teleop) {
				Robot.driveTrain.driveLeftCurved(Math.abs(leftSpeed) > 0.15 ? leftSpeed * (fullPower ? 1 : 0.75) : 0);
				Robot.driveTrain
						.driveRightCurved(Math.abs(rightSpeed) > 0.15 ? rightSpeed * (fullPower ? 1 : 0.75) : 0);
			}
		}
	}

	// Make this return true when this Command no longer needs to run execute()
	protected boolean isFinished() {
		return false;
		// return Robot.oi.getDriverJoystick().getRawButton(8) ||
		// Robot.oi.getDriverJoystick().getRawButton(9) ||
		// Robot.oi.getDriverJoystick().getRawButton(10);
	}

	// Called once after isFinished returns true
	protected void end() {
		Robot.driveTrain.driveLeft(0);
		Robot.driveTrain.driveRight(0);
		Robot.climber.set(0);
		// Robot.oi.getDriverJoystick().setRumble(RumbleType.kLeftRumble, 0);
		// Robot.oi.getDriverJoystick().setRumble(RumbleType.kRightRumble, 0);
	}

	// Called when another command which requires one or more of the same
	// subsystems is scheduled to run
	protected void interrupted() {
		Robot.driveTrain.driveLeft(0);
		Robot.driveTrain.driveRight(0);
		Robot.climber.set(0);
		// Robot.oi.getDriverJoystick().setRumble(RumbleType.kLeftRumble, 0);
		// Robot.oi.getDriverJoystick().setRumble(RumbleType.kRightRumble, 0);
	}
}
