package com.midcoastmaneiacs.Steamworks;

import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.MMAccessProxy;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * <p>Not the wpilib {@link edu.wpi.first.wpilibj.command.Scheduler Scheduler}. This is intended to use static timing
 * rather than relying on the {@link edu.wpi.first.wpilibj.IterativeRobot#robotPeriodic() timing of the periodic}
 * functions, among other benefits.
 *
 * <p>Instantiating Scheduler can act as a {@link TimerTask} for timers, all instances will simply call
 * {@link Scheduler#tick() Scheduler.tick()}. Commands MUST override {@link Command#start()}. If this isn't possible
 * they MUST NOT be started with {@link Command#start()}, use {@link Scheduler#add(Command) Scheduler.add()} instead.
 *
 * <p>However, {@link Command#cancel()} still works to cancel commands, without overrides.
 */
@SuppressWarnings("WeakerAccess")
public class Scheduler extends TimerTask {
	public static boolean enabled = false;
	private static List<Command> schedule = new ArrayList<>();
	public static Command currentCommand;

	/**
	 * Runs commands, similar to wpilib's {@link edu.wpi.first.wpilibj.command.Scheduler#run() getInstance().run()}.
	 */
	public static void tick() {
		for (int i = 0; i < schedule.size(); i++) {
			Command command = schedule.get(i);
			currentCommand = command;

			// A) allows code to "disable" and B) prevents commands from getting cancelled, just "paused"
			// I expect the Robot ...init() methods to handle commands being cancelled
			// However, add an exception to allow commands to end if they are cancelled
			if (!enabled && !command.isCanceled() && !command.willRunWhenDisabled()) continue;

			// check if the command is cancelled, as command.run won't check if it's cancelled after initialize() and
			// execute() are called, until the next run, so this allows a command to immediately cancel itself in favor
			// of running a different command without relying on isFinished()
			if (!MMAccessProxy.runCommand(command) || command.isCanceled()) {
				schedule.remove(i);
				i--;
				MMAccessProxy.commandRemoved(command);
				// ensure that commands have relinquished their control
				for (MMSubsystem j: Robot.subsystems)
					j.relinquishControl(command);
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

	/**
	 * Starts running a command.
	 *
	 * @param command Command to add to the schedule
	 */
	public static void add(Command command) {
		schedule.add(command);
		MMAccessProxy.startRunningCommand(command);
	}

	/**
	 * Runs {@link Scheduler#tick() Scheduler.tick()}, used for TimerTask implementation.
	 */
	@Override
	public void run() {
		tick();
	}

	/**
	 * Cancel all commands (except {@link Notifier}, currently used when robot is disabled during testing or a demonstration.
	 */
	public static void cancelAllCommands() {
		for (Command i: schedule)
			if (!(i instanceof Notifier)) // Don't cancel notifying the driver
				i.cancel();
		for (MMSubsystem i: Robot.subsystems)
			// ensure that no command has control of the subsystems, this will also reset the driveTrain autopilot
			i.takeControl(null);
	}

	/**
	 * A RuntimeException that indicates that some function which is required to be called by a {@link Command} was
	 * called outside of a command. For a command to be detected, that command must be running through the
	 * {@link Scheduler Midcoast Maineiacs Scheduler}.
	 */
	public static class CommandNotFoundException extends RuntimeException {
		public CommandNotFoundException(String message) {
			super(message);
		}
	}
}
