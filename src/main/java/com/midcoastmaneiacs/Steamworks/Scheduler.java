package com.midcoastmaneiacs.Steamworks;

import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.MMAccessProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * Not the wpilib scheduler. This is intended to use static timing rather than relying on the timing of the periodic
 * functions, among other benefits.
 * Instantiating Scheduler can act as a TimerTask for timers, all instances will simply call static Scheduler.tick().
 * Commands MUST override command.start(). If this isn't possible they MUST NOT be started with command.start(), use
 * Scheduler.add(command) instead. However, command.cancel() still works to cancel commands, without overrides.
 */
public class Scheduler extends TimerTask {
	public static boolean enabled = false;
	private static List<Command> schedule = new ArrayList<>();
	public static Command currentCommand;

	/**
	 * Runs commands, similar to wpilib's getInstance().run().
	 */
	public static void tick() {
		for (int i = 0; i < schedule.size(); i++) {
			Command command = schedule.get(i);
			currentCommand = command;

			// A) allows code to "disable" and B) prevents commands from getting cancelled, just "paused"
			// I expect the Robot ...init() methods to handle commands being cancelled
			// However, add an exception to allow commands to end if they are cancelled
			if (!enabled && !command.isCanceled() && !command.willRunWhenDisabled()) continue;

			if (!MMAccessProxy.runCommand(command)) {
				schedule.remove(i);
				i--;
				MMAccessProxy.commandRemoved(command);
			}
		}
		currentCommand = null;
		if (enabled && Robot.driveTrain.getState() == DriveTrain.State.AUTOPILOT)
			Robot.driveTrain.updateAutopilot();
		if (!enabled) { // since commands won't stop motors when paused, we stop them ourselves
			for (MMSubsystem i: Robot.subsystems)
				i.stop();
		}
	}

	public static void add(Command command) {
		schedule.add(command);
		MMAccessProxy.startRunningCommand(command);
	}

	/**
	 * Runs Scheduler.tick(), used for TimerTask implementation.
	 */
	@Override
	public void run() {
		tick();
	}

	/**
	 * Cancel all commands, currently used when robot is disabled during testing or a demonstration.
	 */
	public static void cancelAllCommands() {
		for (Command i: schedule)
			i.cancel();
		for (MMSubsystem i: Robot.subsystems)
			// ensure that no command has control of the subsystems, this will also reset the driveTrain autopilot
			i.takeControl(null);
	}

	/**
	 * A RuntimeException that indicates that some function which is required to be called by a Command was called
	 * outside of a command.
	 */
	public static class CommandNotFoundException extends RuntimeException {
		public CommandNotFoundException(String message) {
			super(message);
		}
	}
}
