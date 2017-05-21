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
 * Commands MUST NOT be started with command.start(), use Scheduler.add(command) instead. However, command.cancel() still
 * works to cancel commands.
 */
public class Scheduler extends TimerTask {
	public static boolean enabled = false;
	private static List<Command> schedule = new ArrayList<>();

	/**
	 * Runs commands, similar to wpilib's getInstance().run().
	 */
	public static void tick() {
		for (int i = 0; i < schedule.size(); i++) {
			Command command = schedule.get(i);
			// A) allows code to "disable" and B) prevents commands from getting cancelled, just "paused"
			// I expect the Robot ...init() methods to handle commands being cancelled
			if (!enabled && !command.willRunWhenDisabled()) continue;

			if (!MMAccessProxy.runCommand(command)) {
				i--;
				schedule.remove(i);
				MMAccessProxy.commandRemoved(command);
			}
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
	}
}
