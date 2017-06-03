package com.midcoastmaneiacs.Steamworks.auto;

import com.midcoastmaneiacs.Steamworks.MMSubsystem;
import com.midcoastmaneiacs.Steamworks.Robot;
import com.midcoastmaneiacs.Steamworks.Scheduler;
import edu.wpi.first.wpilibj.command.Command;

/**
 * Just like a wpilib command, but adds some new features.
 * <ul><li>
 *     Integrates with MMSubsystem to handle inheritance of command control
 * </li><li>
 *     Adds default isFinished() and end() implementations
 * </li><li>
 *     Modifies start() implementation to properly use Scheduler (and detect if it is started by a parent command)
 * </li></ul>
 * For the purposes of this project, a {@link Command} that is also an MMCommand (such as a {@link DriveCommand}) is
 * considered an "active command," while a {@link Command} that is <em>not</em> an MMCommand (such as a
 * {@link com.midcoastmaneiacs.Steamworks.Notifier Notifier}) is considered a "passive command."
 */
public abstract class MMCommand extends Command {
	private MMCommand parent;

	public Command getParent() {
		return parent;
	}

	public boolean controls(MMSubsystem subsystem) {
		return subsystem.directlyControlledBy(this) || this.parent != null && parent.controls(subsystem);
	}

	@Override
	public void start() {
		Scheduler.add(this);
		if (Scheduler.currentCommand instanceof MMCommand)
			parent = (MMCommand) Scheduler.currentCommand;
		else
			parent = null;
	}

	/**
	 * Traverses all subsystems, finding ones controlled by this command, and relinquishes control and stops the
	 * subsystem. This implementation should suffice in many cases, but if you choose to override it, you should call
	 * super.end() if the command controls subsystems.
	 */
	@Override
	protected void end() {
		for (MMSubsystem i: Robot.subsystems)
			if (i.relinquishControl(this))
				i.stop();
	}

	/**
	 * @return If the command has been timed out, or the kill switch has been activated. This should suffice in many
	 * cases, but if you choose to override it, you should reference super.isFinished().
	 */
	@Override
	protected boolean isFinished() {
		return isTimedOut() || Robot.killSwitch();
	}
}
