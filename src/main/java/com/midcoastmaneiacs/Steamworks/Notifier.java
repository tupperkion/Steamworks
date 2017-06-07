package com.midcoastmaneiacs.Steamworks;

import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.command.Command;

/**
 * Notifies the driver by causing 2 pulses to the joystick. If a Notifier is already running, will cancel itself and the
 * first Notifier will add one more pulse.
 */
public class Notifier extends Command {
	private static int timeLeft = 0;

	public Notifier() {
		this.setRunWhenDisabled(true);
	}

	@Override
	public void initialize() {
		if (timeLeft <= 0) {
			timeLeft = 49;
		} else {
			timeLeft += 20;
			this.cancel();
		}
	}

	@Override
	public void execute() {
		if (this.isCanceled()) return; // fun fact: if initialize() calls cancel(), execute() will still run once
		if (timeLeft % 20 < 10) {
			Robot.joystick.setRumble(GenericHID.RumbleType.kLeftRumble, 0);
			Robot.joystick.setRumble(GenericHID.RumbleType.kRightRumble, 0);
		} else {
			Robot.joystick.setRumble(GenericHID.RumbleType.kLeftRumble, 1);
			Robot.joystick.setRumble(GenericHID.RumbleType.kRightRumble, 1);
		}
		timeLeft--;
	}

	@Override
	public boolean isFinished() {
		return timeLeft <= 0;
	}

	@Override
	public void end() {
		if (timeLeft < 0)
			timeLeft = 0;
		Robot.joystick.setRumble(GenericHID.RumbleType.kLeftRumble, 0);
		Robot.joystick.setRumble(GenericHID.RumbleType.kRightRumble, 0);
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	public static boolean isNotifying() {
		return timeLeft > 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * Uses the {@link Scheduler Midcoast Maineiacs Scheduler}.
	 */
	@Override
	public void start() {
		Scheduler.add(this);
	}
}
