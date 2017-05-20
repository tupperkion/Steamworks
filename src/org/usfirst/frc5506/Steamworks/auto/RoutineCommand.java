package org.usfirst.frc5506.Steamworks.auto;

import org.usfirst.frc5506.Steamworks.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 * syntax: command1;command2;/command3:0.3;command4:/1:5;command5;...lastCommand
 *
 * Use ":" to split arguments, e.g. "command:2:3" runs "command" with 2 as arg1 and 3 as arg2
 * For example, to run the RunLeft(speed, time) command with 1 as speed and 2 as time, "RunLeft:1:2"
 *
 * Use ";" to seperate auto, e.g. to run "command1"
 * then "command2", use "command1;command2" Use "/" before a command to run in parallel, e.g. to run "command1", then run "command2" and "command3" at the same time (after "command1"), use "command1;/command2:command3"
 *
 * Don't put "/" before your last command, there is no reason to.
 *
 * Commands:
 *
 * 	Drive(power, time)
 * 		Drive motors for `time` seconds.
 * 	Run(left, right)
 * 		Sets power of left and right motor, according to arguments. Command immediately ends.
 * 	TurnLeft(power, angle)
 *		Runs RIGHT motor until robot is `angle` degrees further LEFT. STOPS LEFT MOTOR.
 * 	TurnRight(power, angle)
 *		Runs RIGHT motor until robot is `angle` degrees further RIGHT. STOPS RIGHT MOTOR.
 *	StopLeft(angle)
 *		Waits until robot is at least `angle - 5` degrees further LEFT, then stops all motors.
 *	StopRight(angle)
 *		Waits until robot is at least `angle - 5` degrees further RIGHT, then stops all motors.
 *	[Gear]
 *		Deposits a gear, scanning left if vision iznt gud. Scans right instead if either of the following conditions are met:
 *			 - Player Station is Blue1, Red1, or Red2 (note that Blue2 still scans left)
 *			 - Driver Station position selector is set to "Left"
 *	[PerfectGear]
 *		Deposits a gear if vision iz gud, does nothing otherwise.
 *	Stop
 *		Waits 0.1 seconds and stops motors (workaround auto being slightly out of sync when run in parallel).
 *	QuickStop
 *		Stops motors immediately.
 *	Wait(time)
 *		Waits `time` seconds.
 *
 * Note: [command] denotes that command is implemented in Routine.java as a part of getCommand();
 *
 * IMPORTANT: auto don't automatically stop DriveTrain motors!
 * Use the Stop* command (or a Turn* command) to stop motors!
 */
public class RoutineCommand extends Command {
	// state variable to be used by auto
	double n;
	boolean done;

	String script;
	double arg;
	double arg2;

	public RoutineCommand(String script, double arg, double arg2) {
		this.script = script;
		this.arg = arg;
		this.arg2 = arg2;
	}

	public void initialize() {
		// ensure that state variables are reset!
		n = 0d;
		done = false;
		switch(script) {
			case("drive"):
				Robot.driveTrain.driveArcade(arg, 0);
				setTimeout(arg2);
				break;
			case("run"):
				Robot.driveTrain.drive(arg, arg2);
				done = true;
				break;
			case("turnleft"):
				Robot.driveTrain.drive(0, 0.6);
				n = Robot.driveTrain.getGyro();
				break;
			case("turnright"):
				Robot.driveTrain.drive(0.6, 0);
			case("stopleft"):
			case("stopright"):
				n = Robot.driveTrain.getGyro();
				break;
			case("stop"):
				setTimeout(0.1); // End the command in 0.1 seconds. Motors will stop when command ends.
				break;
			case("quickstop"):
				done = true; // End the command immediately. Motors will stop when command ends.
				break;
			case("wait"):
				setTimeout(arg);
				break;
			default:
				System.err.println("script '" + script.toUpperCase() + "(" + arg + ", " + arg2 + ") not found!");
				done = true;
				break;
		}
	}

	public void execute() {
		switch(script) {
			case("turnleft"):
				done = n - Robot.driveTrain.getGyro() + arg <= 5;
				break;
			case("turnright"):
				done = Robot.driveTrain.getGyro() - n + arg <= 5;
				break;
		}
	}

	public boolean isFinished() {
		return Robot.joystick.getRawButton(1) || done || isTimedOut();
	}

	public void end() {
		if (script.contains("stop"))
			Robot.driveTrain.driveArcade(0, 0);
	}
}
