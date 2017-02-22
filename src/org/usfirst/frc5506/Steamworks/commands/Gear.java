package org.usfirst.frc5506.Steamworks.commands;

import org.usfirst.frc5506.Steamworks.Robot;
import org.usfirst.frc5506.Steamworks.Vision;

import edu.wpi.first.wpilibj.command.Command;

public class Gear extends Command {
	// degree rotation target
	public double angle = 0d;
	// ticks left. 
	public int timeleft = 0;
	
	public boolean targetFound = false;
	public boolean farAway = true;
	
	public void execute() {
		timeleft--;
		if (targetFound && farAway && !Vision.izgud()) {
			farAway = false;
			setTimeout(0.5);
			// cause break mode to momentarily kick in, maybe slow down a little bit
			Robot.driveTrain.driveArcade(0, 0);
			return;
		}
		if (!targetFound) {
			targetFound = Vision.izgud();
			if (targetFound) {
				double distance = Vision.getDistance();
				angle = (Robot.driveTrain.getGyro() + Vision.getTurningAngle(distance)) % 360;
			}
		} else if (Math.abs((Robot.driveTrain.getGyro() % 360) - angle) < 5) {
			Robot.driveTrain.driveRightCurved(Vision.izgud() ? -0.25 : -0.5);
			Robot.driveTrain.driveLeftCurved(Vision.izgud() ? -0.25 : -0.5);
		} else if (Math.abs((Robot.driveTrain.getGyro() % 360) - angle) < 15) {
			if (Robot.driveTrain.getGyro() % 360 < angle) {
				Robot.driveTrain.driveRightCurved(Vision.izgud() ? -0.15 : -0.25);
				Robot.driveTrain.driveLeftCurved(Vision.izgud() ? -0.25 : -0.5);
			} else {
				Robot.driveTrain.driveRightCurved(Vision.izgud() ? -0.25 : -0.5);
				Robot.driveTrain.driveLeftCurved(Vision.izgud() ? -0.15 : -0.25);
			}
		} else {
			angle = Vision.izgud() ? (Robot.driveTrain.getGyro() + Vision.getTurningAngle()) % 360 : angle;
			if (Robot.driveTrain.getGyro() % 360 < angle) {
				Robot.driveTrain.driveRightCurved(0.3);
				Robot.driveTrain.driveLeftCurved(-0.3);
			} else {
				Robot.driveTrain.driveRightCurved(-0.3);
				Robot.driveTrain.driveLeftCurved(0.3);
			}
		}
	}

	public boolean isFinished() {
		return isTimedOut();
	}
	
	public void end() {
		Robot.driveTrain.driveArcade(0, 0);
	}
	
	public void interrupted() {
		Robot.driveTrain.driveArcade(0, 0);
	}
}
