package com.midcoastmaneiacs.Steamworks;

import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.Subsystem;

/**
 * A Midcoast Maineiacs subsystem, which simply adds a new control-distribution system for commands and teleop control.
 */
public abstract class MMSubsystem extends Subsystem {
	private Command controllingCommand = null;
	private boolean controlledByTeleop = false;

	/**
	 * If true, should only respond to updates if either A) Scheduler.currentCommand == controllingCommand or B)
	 * controllingCommand == null. It is the responsibility of the subclass to [or not to] enforce this rule.
	 */
	boolean enforceControl = true;

	/**
	 * Returns whether or not the Subsystem is controlled by a command.
	 * @param command The command to be checked (or null to check if no command controls it)
	 * @return Whether or not the Subsystem is controlled by the command, or no command if "command" is null
	 */
	public boolean controlledBy(Command command) {
		return controllingCommand == command;
	};

	/**
	 * Grant control of the Subsystem to a command, guaranteeing that it won't be controlled by teleop.
	 * @param command The command, or null to relinguish control to all commands
	 * @param enforce If true, subsystem updates should be ignored unless they are called by the controlling command
	 *                (or there is no controlling command). Should have no effect when command is null.
	 */
	public void takeControl(Command command, boolean enforce) {
		controllingCommand = command;
		enforceControl = enforce;
	}

	/**
	 * Grant control of the Subsystem to a command, guaranteeing that it won't be controlled by teleop. Always enforces
	 * control, so updates should be ignored unless they are called by the controlling command (or there is no
	 * controlling command).
	 * @param command The command, or null to relinguish control to all commands
	 */
	public void takeControl(Command command) {
		takeControl(command, true);
	}

	/**
	 * Relinquish control of Subsystem, only of it was previously controlled by command. More specifically, will call
	 * takeControl(null), so Subsystems may override takeControl, and this method will obey that.
	 */
	public void relinguishControl(Command command) {
		if (controllingCommand == command)
			takeControl(null);
	}

	/**
	 * Returns true if A) teleop for that Subsystem is enabled and B) no Command is currently using the Subsystem.
	 * @return true if teleoperator controls should be allowed to effect the Subsystem.
	 */
	public boolean controlledByTeleop() {
		return controlledByTeleop && controllingCommand != null;
	};

	/**
	 * Checks to make sure that whatever calls this method should be able to control the subsystem. This relies on one
	 * of the following three factors being true:<ul>
	 * <li>the subsystem currently isn't controlled by a command</li>
	 * <li>the method was called by the command that controlled the subsystem</li>
	 * <li>the subsystem was told not to enforce the command control rules when takeControl() was called</li></ul>
	 * @return true if the subsystem should respond to the method that called this method
	 */
	public boolean willRespond() {
		return controlledBy(null) || !enforceControl || controlledBy(Scheduler.currentCommand);
	}

	public void enableTeleop(boolean control) {
		controlledByTeleop = control;
	}

	/**
	 * Stop all motors.
	 */
	public abstract void stop();
}
