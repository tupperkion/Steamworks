package com.midcoastmaineiacs.Steamworks;

import com.midcoastmaineiacs.Steamworks.auto.MMCommand;
import edu.wpi.first.wpilibj.AnalogGyro;
import edu.wpi.first.wpilibj.GenericHID.RumbleType;
import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.command.Command;

@SuppressWarnings("WeakerAccess")
public class DriveTrain extends MMSubsystem {
	public final SpeedController left = new VictorSP(1);
	public final SpeedController right = new VictorSP(2);
	public final AnalogGyro gyro = new AnalogGyro(1);

	private double lastLeftSpeed = 0d;
	private double lastRightSpeed = 0d;
	private static final boolean ACCEL_CURVE = true;

	private State state = State.DISABLED;
	private MMCommand autopilotCommand;

	public double lastLeftRumble = 0d;
	public double lastRightRumble = 0d;

	/**
	 * Gives control to the currently running command and drives the robot, ensuring that the current heading is
	 * maintained. Must be called while a command is running (e.g. in {@link Command#initialize() initialize()}).
	 *
	 * @param speed The desired speed of the robot.
	 * @throws IllegalUseOfAutopilotException if called outside a command.
	 */
	public void setAutopilot(double speed) {
		if (Scheduler.getCurrentCommand() != null && Scheduler.getCurrentCommand() instanceof MMCommand) {
			setState(State.AUTOPILOT);
			autopilotCommand = (MMCommand) Scheduler.getCurrentCommand();
			takeControl((MMCommand) Scheduler.getCurrentCommand());
			wantedHeading = getGyroMod();
			this.speed = speed;
		} else if (Scheduler.getCurrentCommand() != null)
			throw new IllegalUseOfAutopilotException("setAutopilot must not be called by a passive command!");
		else
			throw new IllegalUseOfAutopilotException("setAutopilot must be called by a Command!");
	}

	private void setState(State state) {
		System.out.println("Setting DriveTrain state: " + state);
		if (this.state != state && ((this.state != State.AUTOPILOT && this.state != State.COMMAND) || (state != State.AUTOPILOT && state != State.COMMAND)))
			Robot.notifyDriver();
		if (state != State.AUTOPILOT) autopilotCommand = null;
		this.state = state;
	}

	public State getState() {
		return state;
	}

	public void enableTeleop(boolean control) {
		if (control && state == State.DISABLED)
			setState(State.TELEOP);
		else if (!control && state == State.TELEOP)
			setState(State.DISABLED);
	}

	@Override
	public void takeControl(MMCommand command) {
		if (command != null && state == State.AUTOPILOT && !autopilotInitiatedByCommand(command))
			setState(State.COMMAND);
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

	/**
	 * Checks if this command, or any of its descendants, is the one that initiated autopilot.
	 *
	 * @param command Command to check
	 * @return True if the DriveTrain is in autopilot mode, and this command or an ancestor is the one that set the mode.
	 */
	private boolean autopilotInitiatedByCommand(MMCommand command) {
		if (getState() != State.AUTOPILOT) return false;
		if (autopilotCommand == command) return true;
		for (MMCommand i: command.children) {
			if (autopilotInitiatedByCommand(i)) return true;
		}
		return false;
	}

	@Override
	public boolean relinquishControl(MMCommand command) {
		boolean changed = super.relinquishControl(command);
		// if changed is true, setState will be called anyway so we don't need to call it, hence "!changed" here
		if (!changed && autopilotInitiatedByCommand(command))
			setState(controlledBy(null) ? (controlledByTeleop() ? State.TELEOP : State.DISABLED) : State.COMMAND);
		return changed;
	}

	public DriveTrain() {
		gyro.setSensitivity(2d / 300);
	}

	public void driveLeft(double speed) {
		if (verifyResponse()) {
			lastLeftSpeed = speed;
			left.set(speed);
			lastLeftRumble = Math.abs(speed) >= 0.15 ? Math.abs(speed) : 0d;
			if (!Notifier.isNotifying() && Math.abs(speed) >= 0.15)
				Robot.joystick.setRumble(RumbleType.kLeftRumble, Math.abs(speed));
			else if (!Notifier.isNotifying())
				Robot.joystick.setRumble(RumbleType.kLeftRumble, 0);
		}
	}

	public void driveRight(double speed) {
		if (verifyResponse()) {
			lastRightSpeed = speed;
			right.set(-speed);
			lastRightRumble = Math.abs(speed) >= 0.15 ? Math.abs(speed) : 0d;
			if (!Notifier.isNotifying() && Math.abs(speed) >= 0.15)
				Robot.joystick.setRumble(RumbleType.kRightRumble, Math.abs(speed));
			else if (!Notifier.isNotifying())
				Robot.joystick.setRumble(RumbleType.kRightRumble, 0);
		}
	}

	public void driveLeftCurved(double speed) {
		if (!ACCEL_CURVE) {
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
		if (!ACCEL_CURVE) {
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

	/**
	 * Drives the robot, but changes "forwards" to be in the opposite direction. Used to make auto code more
	 * comprehensible. The same as {@link DriveTrain#drive(double, double)}, but swaps and inverts the left and right
	 * speeds.
	 *
	 * @param left Left speed
	 * @param right Right speed
	 */
	public void driveBackwards(double left, double right) {
		drive(-right, -left);
	}

	public void drive(double speed) {
		drive(speed, speed);
	}

	@Override
	public boolean willRespond() {
		boolean autopilot = false;
		if (state == State.AUTOPILOT && Scheduler.enabled) {
			StackTraceElement[] stack = Thread.currentThread().getStackTrace();
			for (StackTraceElement i: stack)
				if (i.getMethodName().equals("updateAutopilot")) {
					autopilot = true;
					break;
				}
		}
		return autopilot || super.willRespond();
	}

	@Override
	public void stop() {
		left.set(0);
		right.set(0);
		if (!Notifier.isNotifying()) {
			Robot.joystick.setRumble(RumbleType.kRightRumble, 0);
			Robot.joystick.setRumble(RumbleType.kLeftRumble, 0);
		}
		lastLeftRumble = lastRightRumble = 0d;
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
	 * @param target target (will be normalized between 0 and 360)
	 * @return Angle between -180 and 180, inclusive
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

	public double wantedHeading = 0;
	public double speed = 0;
	//** desired turning radius of robot, positive = right turn, negative = left, Double.MAX_VALUE for no turning. */
	//public double turningRadius = Double.MAX_VALUE;

	//** Distance between middles of two wheels */
	//public final double ROBOT_WIDTH = 26;

	/** How much power per degree of error should we change the motor speed? */
	@SuppressWarnings("FieldCanBeLocal")
	private static final double STABILIZATION_CONSTANT = 0.01;

	public void updateAutopilot() { // TODO: test positive and negative autopilot
		double error = getTurningAngle(wantedHeading);
		double left = speed;
		double right = speed;
		left += error * STABILIZATION_CONSTANT;
		right -= error * STABILIZATION_CONSTANT;
		drive(left, right);
	}

	public enum State {
		TELEOP, // teleoperator control
		AUTOPILOT, // controlled by updateAutopilot() (called by Scheduler)
		COMMAND, // controlled by command
		DISABLED // disabled
	}
}

class IllegalUseOfAutopilotException extends Scheduler.CommandNotFoundException {
	public IllegalUseOfAutopilotException(String message) {
		super(message);
	}
}
