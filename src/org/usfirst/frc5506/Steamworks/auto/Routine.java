package org.usfirst.frc5506.Steamworks.auto;

import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.CommandGroup;

/**
 * See RoutineCommand.java for documentation.
 */
public class Routine extends CommandGroup {
	public Routine(String script) {
		String[] commands = script.toLowerCase().split(";");
		for (String i: commands) {
			// should I run in parallel?
			boolean parallel = false;
			if (i.charAt(0) == '/') {
				i = i.substring(1);
				parallel = true;
			}

			// separate command from arguments
			String[] split = i.split(":");
			double arg1 = 0d;
			double arg2 = 0d;
			if (split.length >= 2)
				arg1 = new Double(split[1]);
			if (split.length >= 3)
				arg2 = new Double(split[1]);

			// run the command
			if (parallel)
				addParallel(getCommand(split[0], arg1, arg2));
			else
				addSequential(getCommand(split[0], arg1, arg2));
		}
	}

	/**
	 * Finds the correct command corresponding to a given command.
	 * @param script	name of the command
	 * @param arg		first argument
	 * @param arg2		second argument
	 *
	 * @return	Command
	 */
	public Command getCommand(String script, double arg, double arg2) {
		switch(script.toLowerCase()) {
			case("gear"):
				return new Gear(true);
			case("perfectgear"):
				return new Gear(false);
			default:
				return new RoutineCommand(script.toLowerCase(), arg, arg2);
		}
	}

	/**
	 * Same as `getCommand(script, arg, arg2)` but uses 0.0 as arg and arg2
	 */
	public Command getCommand(String script) {
		return getCommand(script, 0d, 0d);
	}
}