package com.midcoastmaneiacs.Steamworks;

import edu.wpi.first.wpilibj.AnalogGyro;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.command.Command;

public class DriveTrain extends MMSubsystem {
	public final SpeedController left = new VictorSP(1);
	public final SpeedController right = new VictorSP(2);
	public final AnalogGyro gyro = new AnalogGyro(0);

	private double lastLeftSpeed = 0d;
	private double lastRightSpeed = 0d;
	private final boolean accelCurve = true;

	private State state = State.DISABLED;

	/**
	 * Gives control to the currently running command and drives the robot, ensuring that the current heading is
	 * maintained. Must be called while a command is running (e.g. in initialize()).
	 * @param speed The desired speed of the robot.
	 * @throws IllegalUseOfAutopilotException if called outside a command.
	 */
	public void setAutopilot(double speed) {
		if (Scheduler.currentCommand != null) {
			setState(State.AUTOPILOT);
			takeControl(Scheduler.currentCommand);
			wantedHeading = getGyroMod();
			this.speed = speed;
		} else
			throw new IllegalUseOfAutopilotException("setAutopilot must be called by a Command!");
	}

	private void setState(State state) {
		this.state = state;
	}

	public State getState() {
		return state;
	}

	@Override
	public void enableTeleop(boolean control) {
		super.enableTeleop(control);
		if (control && state == State.DISABLED)
			setState(State.TELEOP);
		else if (!control && state == State.TELEOP)
			setState(controlledBy(null) ? State.DISABLED : State.COMMAND);
	}

	@Override
	public void takeControl(Command command) {
		super.takeControl(command);
		if (command != null && state != State.AUTOPILOT && state != State.COMMAND)
			setState(State.COMMAND);
		else if (command == null) {
			if (controlledByTeleop() && state != State.TELEOP)
				setState(State.TELEOP);
			else if (!controlledByTeleop())
				setState(State.DISABLED);
		}
	}

	public DriveTrain() {
		gyro.setSensitivity(0.007); // idk I found this in RobotMap so it's here now
	}

	public void driveLeft(double speed) {
		if (willRespond()) {
			lastLeftSpeed = speed;
			left.set(speed);
			if (!Notifier.isNotifying() && Math.abs(speed) >= 0.15 && Robot.rumble.getSelected())
				Robot.joystick.setRumble(RumbleType.kLeftRumble, Math.abs(speed));
			else if (!Notifier.isNotifying())
				Robot.joystick.setRumble(RumbleType.kLeftRumble, 0);
		}
	}

	public void driveRight(double speed) {
		if (willRespond()) {
			lastRightSpeed = speed;
			right.set(-speed);
			if (!Notifier.isNotifying() && Math.abs(speed) >= 0.15 && Robot.rumble.getSelected())
				Robot.joystick.setRumble(RumbleType.kRightRumble, Math.abs(speed));
			else if (!Notifier.isNotifying())
				Robot.joystick.setRumble(RumbleType.kRightRumble, 0);
		}
	}

	public void driveLeftCurved(double speed) {
		if (!accelCurve) {
			driveLeft(speed);
			return;
		}
		if (speed > 0) {
			if (speed > lastLeftSpeed + 0.04)
				driveLeft(lastLeftSpeed + 0.04);
			else
				driveLeft(speed);
		} else {
			// if (speed < lastLeftSpeed - 0.04)
			// left(lastLeftSpeed - 0.04);
			// else
			driveLeft(speed);
		}
	}

	public void driveRightCurved(double speed) {
		if (!accelCurve) {
			driveRight(speed);
			return;
		}
		if (speed > 0) {
			if (speed > lastRightSpeed + 0.04)
				driveRight(lastRightSpeed + 0.04);
			else
				driveRight(speed);
		} else {
			// if (speed < lastRightSpeed - 0.04)
			// right(lastRightSpeed - 0.04);
			// else
			driveRight(speed);
		}
	}

	public void driveArcade(double forward, double turn) {
		drive(forward + turn, forward - turn);
	}

	public void drive(double left, double right) {
		if (Math.abs(left) > 1) {
			left *= Math.abs(1 / left);
			right *= Math.abs(1 / left);
		}
		if (Math.abs(right) > 1) {
			left *= Math.abs(1 / right);
			right *= Math.abs(1 / right);
		}
		driveLeft(left);
		driveRight(right);
	}

	@Override
	public void stop() {
		drive(0, 0);
	}

	public double getGyro() {
		return gyro.getAngle();
	}

	/**
	 * @return Angle between 0 (inclusive) and 360 (exclusive)
	 */
	public double getGyroMod() {
		return getGyro() % 360;
	}

	/**
	 * Calculates how far (and which way) to turn to match a target angle
	 *
	 * @param target
	 *            target (will be normalized between 0 and 360)
	 * @return angle (between -180 and 180, inclusive)
	 */
	public double getTurningAngle(double target) {
		target = target % 360;
		double current = getGyroMod();
		if (Math.abs(target - current) <= 180) {
			return target - current;
		} else if (target < current) {
			return target + 360 - current;
		} else {
			return target - 360 - current;
		}
	}

	@Override
	protected void initDefaultCommand() {}

	public double wantedHeading = 0;
	public double speed = 0;
	//** desired turning radius of robot, positive = right turn, negative = left, Double.MAX_VALUE for no turning. */
	//public double turningRadius = Double.MAX_VALUE;

	//** Distance between middles of two wheels */
	//public final double ROBOT_WIDTH = 26;

	/** How much power per degree of error should we change the motor speed? */
	private final double STABILIZATION_CONSTANT = 0.01;

	public void updateAutopilot() {
		double error = getTurningAngle(wantedHeading);
		double left = speed;
		double right = speed;
		/*if (turningRadius != Double.MAX_VALUE) {
			left = speed * (1 + (ROBOT_WIDTH / (2 * turningRadius)));
			right = speed - (1 + (ROBOT_WIDTH / (2 * turningRadius)));
		}(*/
		left += error * STABILIZATION_CONSTANT;
		right -= error * STABILIZATION_CONSTANT;
		drive(left, right);
	}

	public enum State {
		TELEOP, // teleoperator control
		AUTOPILOT, // controlled by autopilot() method in DriveTrain.java
		COMMAND, // controlled by command
		DISABLED // disabled
	}
}

class IllegalUseOfAutopilotException extends Scheduler.CommandNotFoundException {
	public IllegalUseOfAutopilotException(String message) {
		super(message);
	}
}
