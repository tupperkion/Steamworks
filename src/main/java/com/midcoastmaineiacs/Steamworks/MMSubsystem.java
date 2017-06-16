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
	 * If true, should only respond to updates if either A) Scheduler.currentCommand == controllingCommand or B)
	 * controllingCommand == null. It is the responsibility of the subclass to [or not to] enforce this rule. Using
	 * willRespond() is generally the easiest way to enforce this.
	 */
	boolean enforceControl = true;

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
	 * Returns whether or not the Subsystem is directing controlled by a command. (Controlled by it, and not one of its
	 * parents)
	 *
	 * @param command The command to be checked (or null to check if no command controls it)
	 * @return Whether or not the Subsystem is controlled by the command, or no command if "command" is null
	 */
	public boolean directlyControlledBy(Command command) {
		return controllingCommand == command;
	}

	/**
	 * Grant control of the Subsystem to a command and all of its children (any command started by it),
	 * guaranteeing that it won't be controlled by teleop.
	 *
	 * @param command The command, or null to relinquish control to all commands
	 * @param enforce If true, subsystem updates should be ignored unless they are called by the controlling command
	 *                (or there is no controlling command). Should have no effect when command is null.
	 */
	public void takeControl(MMCommand command, boolean enforce) {
		System.out.println(this + ": Taking control: " + command);
		if (command == null || !command.controls(this))
			controllingCommand = command;
		else
			System.out.println("-- nope, it already controls me!");
		enforceControl = enforce;
	}

	/**
	 * Grant control of the Subsystem to a command, guaranteeing that it won't be controlled by teleop. Always enforces
	 * control, so updates should be ignored unless they are called by the controlling command (or there is no
	 * controlling command).
	 *
	 * @param command The command, or null to relinguish control to all commands
	 */
	public void takeControl(MMCommand command) {
		takeControl(command, true);
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
	 * </li><li>
	 *     the subsystem was told not to enforce the command control rules when takeControl() was called
	 * </li></ul>
	 *
	 * @return true if the subsystem should respond to the method that called this method
	 */
	public boolean willRespond() {
		return Scheduler.enabled && (controlledBy(null) || !enforceControl || Scheduler.getCurrentCommand() instanceof MMCommand && controlledBy((MMCommand) Scheduler.getCurrentCommand()));
	}

	/**
	 * Returns {@link MMSubsystem#willRespond()}, but ensures that the subsystem isn't being controlled by a passive command.
	 * @return the result of {@link MMSubsystem#willRespond()}
	 * @throws Scheduler.IllegalPassiveCommandException if the passive command is being run
	 */
	public boolean verifyResponse() {
		if (willRespond()) {
			StackTraceElement[] stack = Thread.currentThread().getStackTrace();
			if (Scheduler.getCurrentCommand() != null && !(Scheduler.getCurrentCommand() instanceof MMCommand))
				throw new Scheduler.IllegalPassiveCommandException("Passive command cannot control a subsystem!");
			return true;
		}
		return false;
	}

	public void enableTeleop(boolean enabled) {}

	/**
	 * Stop all motors.
	 */
	public abstract void stop();

	/**
	 * This shouldn't be necessary but it's here because wpilib. Does absolutely nothing.
	 */
	@Override
	protected void initDefaultCommand() {}
}
