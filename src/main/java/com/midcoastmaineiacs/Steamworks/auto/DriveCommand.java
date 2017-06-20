package com.midcoastmaineiacs.Steamworks.auto;

import com.midcoastmaineiacs.Steamworks.Robot;

public class DriveCommand extends MMCommand {
	private final boolean autopilot;
	private double left;
	private double right;
	private double speed;
	private final double time;

	/**
	 * Drives the robot straight, using the autopilot gyro stabilization.
	 * @param speed desired speed
	 * @param time  timeout, in seconds
	 */
	public DriveCommand(double speed, double time) {
		autopilot = true;
		this.speed = speed;
		this.time = time;
		requires(Robot.driveTrain);
	}

	/**
	 * Drives the robot, running the left and right motors independently. Gyro stabilization and acceleration curve
	 * aren't utilized.
	 * @param left  left motor speed
	 * @param right right motor speed
	 * @param time  timeout, in seconds
	 */
	public DriveCommand(double left, double right, double time) {
		autopilot = false;
		this.left = left;
		this.right = right;
		this.time = time;
	}

	// Called just before this Command runs the first time
	protected void initialize() {
		/*if (gear) {
			setTimeout(0.25);
			Robot.driveTrain.setAutopilot(0.4);
		} else {
			setTimeout(2);
			Robot.driveTrain.setAutopilot(-0.6);
		}*/
		timeout(time);
		if (autopilot) {
			//Robot.driveTrain.setAutopilot(speed);
			Robot.driveTrain.takeControl(this);
			Robot.driveTrain.drive(speed); // TODO: change to go back to using autopilot
		} else {
			Robot.driveTrain.takeControl(this);
			Robot.driveTrain.drive(left, right);
		}
	}

	@Override
	public void resume() {
		System.out.println("Resuming: " + this);
		if (autopilot) {
			Robot.driveTrain.drive(speed); // TODO: remove
		} else {
			Robot.driveTrain.drive(left, right);
		}
	}

	protected boolean isFinished() {
		return super.isFinished() || !Robot.driveTrain.controlledBy(this);
	}
}
