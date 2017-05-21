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
	 */
	public void takeControl(Command command) {
		controllingCommand = command;
	}

	/**
	 * Relinquish control of Subsystem, only of it was previously controlled by command.
	 */
	public void relinguishControl(Command command) {
		if (controllingCommand == command)
			controllingCommand = null;
	}

	/**
	 * Returns true if A) teleop for that Subsystem is enabled and B) no Command is currently using the Subsystem.
	 * @return true if teleoperator controls should be allowed to effect the Subsystem.
	 */
	public boolean controlledByTeleop() {
		return controlledByTeleop && controllingCommand != null;
	};

	public void enableTeleop(boolean control) {
		controlledByTeleop = control;
	}
}
