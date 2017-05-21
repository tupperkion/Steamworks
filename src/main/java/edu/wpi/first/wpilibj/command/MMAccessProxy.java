package edu.wpi.first.wpilibj.command;

/**
 * This class serves to give the Midcoast Maineiacs access to package-private variables in wpilib.
 */
public class MMAccessProxy {
	public static boolean runCommand(Command command) {
		return command.run();
	}

	public static void startRunningCommand(Command command) {
		command.startRunning();
	}

	public static void commandRemoved(Command command) {
		command.removed();
	}
}
