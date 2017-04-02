package org.usfirst.frc5506.Steamworks.commands;

import org.usfirst.frc5506.Steamworks.Robot;

import edu.wpi.first.wpilibj.command.Command;

/**
 *
 */
public class Surge extends Command {
	public boolean gear;
	
	public Surge() {
		// Use requires() here to declare subsystem dependencies
		// eg. requires(chassis);
		this(false);
	}
	
	public Surge(boolean gear) {
		this.gear = gear;
	}

	// Called just before this Command runs the first time
	protected void initialize() {
		if (gear) {
			setTimeout(0.25);
			Robot.driveTrain.driveArcade(0.4, 0);
		} else {
			setTimeout(2);
			Robot.driveTrain.driveArcade(-0.6, 0);
		}
	}

	// Called repeatedly when this Command is scheduled to run
	protected void execute() {
	}

	// Make this return true when this Command no longer needs to run execute()
	protected boolean isFinished() {
		return isTimedOut();
	}

	// Called once after isFinished returns true
	protected void end() {
		Robot.driveTrain.driveArcade(0, 0);
	}

	// Called when another command which requires one or more of the same
	// subsystems is scheduled to run
	protected void interrupted() {
		Robot.driveTrain.driveArcade(0, 0);
	}
}
