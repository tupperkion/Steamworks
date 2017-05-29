package edu.wpi.first.wpilibj.command;

/**
 * This class serves to give the Midcoast Maineiacs access to package-private variables in wpilib. This allows us to
 * implement a custom scheduler and have more control over command execution.
 */
public class MMAccessProxy {
	/**
	 * Provides public access to {@link Command#run()}.
	 *
	 * @see Command#run()
	 * @see Command#startRunning()
	 */
	public static boolean runCommand(Command command) {
		return command.run();
	}

	/**
	 * Provides public access to {@link Command#startRunning()}.
	 *
	 * @see Command#startRunning()
	 */
	public static void startRunningCommand(Command command) {
		command.startRunning();
	}

	/**
	 * Provides public access to {@link Command#removed()}.
	 *
	 * @see Command#removed()
	 * @see Command#startRunning()
	 */
	public static void commandRemoved(Command command) {
		command.removed();
	}
}
