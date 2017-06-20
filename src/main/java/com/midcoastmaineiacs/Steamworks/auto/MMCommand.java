package com.midcoastmaineiacs.Steamworks.auto;

import com.midcoastmaineiacs.Steamworks.MMSubsystem;
import com.midcoastmaineiacs.Steamworks.Notifier;
import com.midcoastmaineiacs.Steamworks.Robot;
import com.midcoastmaineiacs.Steamworks.Scheduler;
import edu.wpi.first.wpilibj.command.Command;

import java.util.ArrayList;
import java.util.List;

/**
 * Just like a wpilib command, but adds some new features.
 * <ul><li>
 *     Integrates with MMSubsystem to handle inheritance of command control
 * </li><li>
 *     Adds default isFinished() and end() implementations
 * </li><li>
 *     Modifies start() implementation to properly use Scheduler (and detect if it is started by a parent command)
 * </li><li>
 *     Adds a fully-fledged command hierarchy system
 * </li></ul>
 * For the purposes of this project, a {@link Command} that is also an MMCommand (such as a {@link DriveCommand}) is
 * considered an "active command," while a {@link Command} that is <em>not</em> an MMCommand (such as a
 * {@link Notifier Notifier}) is considered a "passive command."
 */
@SuppressWarnings("WeakerAccess")
public abstract class MMCommand extends Command {
	/** Parent of the command. Should only be modified internally by MMCommand and by {@link Scheduler}. */
	public MMCommand parent;
	/** Only to be used by the scheduler */
	public boolean enabled = true;
	/** How much time is left to run the command, not counting disabled time */
	private double timeLeft = -1;
	public boolean hasRun = true;

	public boolean controls(MMSubsystem subsystem) {
		return subsystem.directlyControlledBy(this) || this.parent != null && parent.controls(subsystem);
	}

	protected boolean requireChildren = false;
	/** List of commands spawned (directly or indirectly) by this command. */
	public List<MMCommand> children = new ArrayList<>();

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
	 * Called by scheduler whenever the command is started. Should only be called by the scheduler.
	 */
	public final void _start() {
		requireChildren = false;
		children.clear();
		timeLeft = -1;
		if (Scheduler.getCurrentCommand() instanceof MMCommand) {
			parent = (MMCommand) Scheduler.getCurrentCommand();
			if (parent != null)
				parent.children.add(this);
		} else if (Scheduler.getCurrentCommand() != null)
			throw new Scheduler.IllegalPassiveCommandException("Passive command cannot start an active command!\n" +
																   "Modify the command class to extend MMCommand!");
		else
			parent = null;
		hasRun = false;
	}

	/**
	 * Traverses all subsystems, finding ones controlled by this command, and relinquishes control and stops the
	 * subsystem. This implementation should suffice in many cases, but if you choose to override it, you should call
	 * super.end() if the command controls subsystems.
	 */
	@Override
	protected void end() {
		for (MMSubsystem i: Robot.subsystems)
			if (i.controlledBy(this)) {
				i.relinquishControl(this);
				i.stop();
			}
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
	 * @return If the kill switch has been activated, or the parent has been stopped. Use this if you want to override
	 * {@link MMCommand#isFinished()} to follow these rules without cancelling on timeout.
	 */
	protected boolean shouldCancel() {
		if (requireChildren) {
			boolean ok = false;
			for (MMCommand i : children)
				if (i.isRunning() && !i.isCanceled()) {
					ok = true;
					break;
				}
			if (!ok) return true;
		}
		return Robot.killSwitch() || parent != null && !parent.isRunning() && parent.isCanceled();
	}

	/**
	 * After called, {@link MMCommand#shouldCancel()} will cancel the command if there are no running children commands.
	 */
	public void releaseForChildren() {
		requireChildren = true;
	}

	/**
	 * Called when a command is resumed, after the scheduler has been disabled, and re-enabled. Assume all subsystems have
	 * been stopped by the scheduler at this point.
	 */
	public void resume() {}

	/**
	 * Sets a timeout in such a way that it will be paused when the command is frozen (e.g. by a disable). This should
	 * be used instead of {@link Command#setTimeout(double) setTimeout()} most of the time.
	 *
	 * @param time Time to expire the command, in seconds.
	 * @see Command#setTimeout(double)
	 */
	public void timeout(double time) {
		if (enabled)
			setTimeout(time);
		timeLeft = time;
	}

	/**
	 * Called by scheduler when state of enabled changes, used to pause/resume timeouts
	 */
	public void updateTimeout() {
		if (timeLeft != -1) {
			if (!Scheduler.enabled) {
				timeLeft = timeLeft - timeSinceInitialized();
				setTimeout(Double.MAX_VALUE);
			} else {
				setTimeout(timeLeft + timeSinceInitialized());
				timeLeft = timeLeft + timeSinceInitialized();
			}
		}
	}
}
