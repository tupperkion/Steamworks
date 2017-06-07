package com.midcoastmaneiacs.Steamworks;

import com.midcoastmaneiacs.Steamworks.auto.MMCommand;
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
 * {@link MMCommand} overrides {@link Command#start() start()}, so active commands should not have to worry about this,
 * but passive commands must override {@link Command#start() start} (see {@link Notifier#start()}).
 *
 * <p>However, {@link Command#cancel()} still works to cancel commands, without overrides.
 */
@SuppressWarnings("WeakerAccess")
public class Scheduler extends TimerTask {
	public static boolean enabled = false;
	private static final List<Command> schedule = new ArrayList<>();
	public static Command currentCommand;

	/**
	 * Runs commands, similar to wpilib's {@link edu.wpi.first.wpilibj.command.Scheduler#run() getInstance().run()}.
	 */
	public static void tick() {
		for (int i = 0; i < schedule.size(); i++) {
			Command command = schedule.get(i);
			currentCommand = command;

			// A) allows code to "disable" and B) prevents commands from getting cancelled, just "paused"
			// I expect the Robot ...init() methods to cancel commands as necessary
			// However, add an exception to allow commands to end if they are cancelled
			if (!enabled && !command.isCanceled() && !command.willRunWhenDisabled()) continue;

			// check if the command is cancelled, as command.run won't check if it's cancelled after initialize() and
			// execute() are called, until the next run, so this allows a command to immediately cancel itself in favor
			// of running a different command without relying on isFinished()
			if (!MMAccessProxy.runCommand(command) || command.isCanceled()) {
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

	/**
	 * Starts running a command. Called by {@link MMCommand#start()}. <em>Must not</em> be called (directly or
	 * indirectly) by a passive command. <em>Can</em> be called by an active command, or outside of any command all
	 * together.
	 *
	 * <p>Passive commands <em>must</em> override {@link Command#start()} such that it uses this method (e.g.
	 * {@code Scheduler.add(this);}).
	 *
	 * @param command Command to add to the schedule
	 * @throws Scheduler.IllegalPassiveCommandException if called by a passive command
	 * @see MMCommand#start()
	 */
	public static void add(Command command) {
		if (currentCommand instanceof MMCommand)
			((MMCommand) command).parent = (MMCommand) currentCommand;
		else if (currentCommand != null)
			throw new Scheduler.IllegalPassiveCommandException("Passive command cannot start an active command!\n" +
																   "Modify the command class to extend MMCommand!");
		else
			((MMCommand) command).parent = null;
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
			if (i instanceof MMCommand) // Don't cancel passive commands such as the Notifier
				i.cancel();
		for (MMSubsystem i: Robot.subsystems)
			// ensure that no command has control of the subsystems, this will also reset the driveTrain autopilot
			i.takeControl(null);
	}

	/**
	 * A RuntimeException that indicates illegal handling of commands.
	 */
	public static class CommandException extends RuntimeException {
		public CommandException(String message) {
			super(message);
		}
	}

	/**
	 * A {@link CommandException} that indicates that some function which is required to be called by a {@link Command} was
	 * called outside of a command. For a command to be detected, that command must be running through the
	 * {@link Scheduler Midcoast Maineiacs Scheduler}.
	 */
	public static class CommandNotFoundException extends CommandException {
		public CommandNotFoundException(String message) {
			super(message);
		}
	}

	/**
	 * A {@link CommandException} that indicates that a passive command did something that is only permissible to an
	 * active command. A "passive command" is one that is a {@link Command} but not an {@link MMCommand} while an "active
	 * command" <em>is</em> an {@link MMCommand}.
	 *
	 * <p><em>Passive</em> commands <b>may not</b>:
	 * <ul><li>
	 *     Control (directly or indirectly) a subsystem
	 * </li><li>
	 *     Start an active command
	 * </li></ul>
	 * If a Command needs to do either of these, they <em>must</em> be considered active commands and extend MMCommand.
	 */
	public static class IllegalPassiveCommandException extends RuntimeException {
		public IllegalPassiveCommandException(String message) {
			super(message);
		}
	}
}
