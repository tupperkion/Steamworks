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
@SuppressWarnings("WeakerAccess")
public abstract class MMCommand extends Command {
	/** Parent of the command. Should only be modified internally by MMCommand and by {@link Scheduler}. */
	public MMCommand parent;

	public boolean controls(MMSubsystem subsystem) {
		return subsystem.directlyControlledBy(this) || this.parent != null && parent.controls(subsystem);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Uses the {@link Scheduler Midcoast Maineiacs Scheduler}. Passive commands are not allowed to start active
	 * commands, but any command <em>can</em> be started outside of a Command all together.
	 *
	 * @throws Scheduler.IllegalPassiveCommandException if started by a passive command
	 */
	@Override
	public void start() {
		Scheduler.add(this);
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
	 * @return If the command has been timed out, or the kill switch has been activated, or the parent has stopped. This
	 * should suffice in many cases, but if you choose to override it, you should reference super.isFinished() or
	 * {@link MMCommand#shouldCancel()}.
	 */
	@Override
	protected boolean isFinished() {
		return isTimedOut() || shouldCancel();
	}

	/**
	 * @return If the kill switch has been activited, or the parent has been stopped. Use this if you want to override
	 * {@link MMCommand#isFinished()} to follow these rules without cancelling on timeout.
	 */
	protected boolean shouldCancel() {
		return Robot.killSwitch() || parent != null && !parent.isRunning() && parent.isCanceled();
	}
}
