package com.midcoastmaineiacs.Steamworks;

import com.midcoastmaineiacs.Steamworks.auto.MMCommand;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Subsystem;

/**
 * A Midcoast Maineiacs subsystem, which simply adds a new control-distribution system for commands and teleop control.
 */
@SuppressWarnings("WeakerAccess")
public abstract class MMSubsystem extends Subsystem {
	private MMCommand controllingCommand = null;

	/**
	 * Returns whether or not the Subsystem is controlled by a command or one of its parents.
	 *
	 * @param command The command to be checked (or null to check if no command controls it)
	 * @return Whether or not the Subsystem is controlled by the command, or no command if "command" is null
	 */
	public boolean controlledBy(MMCommand command) {
		return controllingCommand == command || command != null && command.controls(this);
	}

	/**
	 * Returns whether or not the Subsystem is directing controlled by a command (controlled by it, and not one of its
	 * parents). In most cases, you should use {@link MMSubsystem#controlledBy(MMCommand)} instead, in order to obey
	 * command hierarchy.
	 *
	 * @param command The command to be checked (or null to check if no command controls it)
	 * @return Whether or not the Subsystem is controlled by the command, or no command if "command" is null
	 */
	public boolean directlyControlledBy(Command command) {
		return controllingCommand == command;
	}

	/**
	 * Grant control of the Subsystem to a command, guaranteeing that it won't be controlled by teleop or another,
	 * unrelated command.
	 *
	 * @param command The command, or null to relinguish control to all commands
	 */
	public void takeControl(MMCommand command) {
		System.out.println(this + ": Taking control: " + command);
		if (command == null || !command.controls(this)) {
			controllingCommand = command;
			stop();
		} else
			System.out.println("-- nope, it already controls me!");
	}

	/**
	 * Relinquish control of Subsystem, only of it was previously controlled by command. More specifically, will call
	 * takeControl(null), so Subsystems may override takeControl, and this method will obey that. Will not relinquish
	 * control if the subsystem is being controlled by the commands
	 * {@link MMCommand#parent parent}.
	 *
	 * @return If the subsystem was previously controlled by this command AND control has been relinquished
	 */
	public boolean relinquishControl(MMCommand command) {
		System.out.println(this + ": Relinquishing control from " + command + " if it's " + controllingCommand);
		if (controllingCommand == command) {
			takeControl(null);
			return true;
		}
		return false;
	}

	/**
	 * Returns true if A) teleop for that Subsystem is enabled and B) no Command is currently using the Subsystem.
	 *
	 * @return true if teleoperator controls should be allowed to effect the Subsystem.
	 */
	public boolean controlledByTeleop() {
		return Scheduler.teleop && controllingCommand == null;
	}

	/**
	 * Checks to make sure that whatever calls this method should be able to control the subsystem. This relies on one
	 * of the following three factors being true:
	 * <ul><li>
	 *     the subsystem currently isn't controlled by a command
	 * </li><li>
	 *     the method was called by the command that controlled the subsystem
	 * </li></ul>
	 *
	 * @return true if the subsystem should respond to the method that called this method
	 */
	public boolean willRespond() {
		return Scheduler.enabled && (controlledBy(null) || Scheduler.getCurrentCommand() instanceof MMCommand && controlledBy((MMCommand) Scheduler.getCurrentCommand()));
	}

	/**
	 * Returns {@link MMSubsystem#willRespond()}, but ensures that the subsystem isn't being controlled by a passive command.
	 * @return the result of {@link MMSubsystem#willRespond()}
	 * @throws Scheduler.IllegalPassiveCommandException if called by a passive command
	 */
	public boolean verifyResponse() {
		if (willRespond()) {
			if (Scheduler.getCurrentCommand() != null && !(Scheduler.getCurrentCommand() instanceof MMCommand))
				throw new Scheduler.IllegalPassiveCommandException("Passive command cannot control a subsystem!");
			return true;
		}
		return false;
	}

	public void enableTeleop(boolean enabled) {}

	/**
	 * Stop all actuators. Does not check {@link MMSubsystem#verifyResponse()}.
	 */
	public abstract void stop();

	/**
	 * This shouldn't be necessary but it's here because wpilib. Does absolutely nothing.
	 */
	@Override
	protected void initDefaultCommand() {}
}
