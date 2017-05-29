package com.midcoastmaneiacs.Steamworks.auto;

import com.midcoastmaneiacs.Steamworks.Robot;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.IllegalUseOfCommandException;

/**
 * Runs multiple commands in parallel. Used as a convenient way to instantiate sequences (especially within a Command)
 * without dedicating a class to a new CommandGroup. Also allows inner commands to be cancelled, unlike a CommandGroup.
 */
public class Series extends Command {
	private Command[] commands;
	private int i = 0;

	public Series(Command... commands) {
		this.commands = commands;
	}

	/**
	 * Add a new command to the sequence. Must <em>not</em> be called while the Series is running.
	 *
	 * @param command Command to add to the list
	 * @throws IllegalUseOfCommandException if called while the Series is running
	 */
	public void add(Command command) {
		if (isRunning()) {
			throw new IllegalUseOfCommandException("Cannot add a Command to a Series while said Series is running.\n" +
				"Make sure you add all required commands before adding a Series to the scheduler.");
		}
		commands[commands.length] = command;
	}

	@Override
	protected void initialize() {
		if (commands.length > 0)
			commands[0].start();
		else
			cancel();
	}

	@Override
	protected void execute() {
		if (isCanceled()) return;
		if (!commands[i].isRunning()) {
			i++;
			if (commands.length > i)
				commands[i].start();
			else
				cancel();
		}
	}

	@Override
	protected void end() {
		if (commands.length > i && commands[i].isRunning())
			commands[i].cancel();
	}

	@Override
	protected boolean isFinished() {
		// cancel if the kill switch is activated
		return Robot.killSwitch();
	}
}
