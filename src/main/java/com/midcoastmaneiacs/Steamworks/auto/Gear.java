package com.midcoastmaneiacs.Steamworks.auto;

import com.midcoastmaneiacs.Steamworks.Robot;
import com.midcoastmaneiacs.Steamworks.Scheduler;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

public class Gear extends Command {
	/**
	 * Scans right instead if either of the following conditions are met:<ul>
	 * <li>Player Station is Blue1, Red1, or Red2 (note that Blue2 still scans left)</li>
	 * <li>Driver Station position selector is set to "Left"</li></ul>
	 */
	public boolean scan;
	// have I found the vision targets?
	public boolean found;
	// is the clock running?
	public boolean timing;

	public Gear(boolean scan) {
		if (scan && !Vision.isalive()) scan = false;
		this.scan = scan;
		found = Vision.izgud();
		timing = false;
	}

	public Gear() {
		this(false);
	}

	public void execute() {
		if (!found) found = Vision.izgud();
		if (scan && !found) {
			if (DriverStation.getInstance().getLocation() == 1 ||
				(DriverStation.getInstance().getLocation() == 2 && DriverStation.getInstance().getAlliance() == DriverStation.Alliance.Red) ||
				Robot.pos.getSelected() == 3) {
				// scan right
				Robot.driveTrain.driveLeft(0);
				Robot.driveTrain.driveRight(-0.4);
			} else {
				// scan left
				Robot.driveTrain.driveRight(0);
				Robot.driveTrain.driveLeft(-0.4);
			}
		}
		if (found) {
			if (Vision.izgud()) {
				//double distance = Vision.getDistance();
				//if (distance > 12) {
					double error = Vision.getTurningAngle();
					SmartDashboard.putNumber("Debug", error);
					if (error < -30) {
						Robot.driveTrain.driveLeft(-0.4);
						Robot.driveTrain.driveRight(0);
					} else if (error < -5) {
						Robot.driveTrain.driveLeft(-0.6);
						Robot.driveTrain.driveRight(-0.4);
					} else if (error < 5) {
						Robot.driveTrain.driveArcade(-0.6, 0);
					} else if (error < 30) {
						Robot.driveTrain.driveRight(-0.6);
						Robot.driveTrain.driveLeft(-0.4);
					} else {
						Robot.driveTrain.driveRight(-0.4);
						Robot.driveTrain.driveRight(0);
					}
				//} else {
				//	if (!timing) {
				//		setTimeout(0.5);
				//		timing = true;
				//	}
				//	Robot.driveTrain.driveArcade(0.4, 0);
				//}
			} else {
				Robot.driveTrain.driveArcade(0.5, 0);
				if (!timing) {
					setTimeout(0.5);
					timing = true;
				}
				Robot.driveTrain.driveArcade(0.4, 0);
			}
		}
	}

	public boolean isFinished() {
		return Robot.joystick.getRawButton(1) || isTimedOut() || !Vision.isalive() || (!scan && !found);
	}

	public void end() {
		Robot.driveTrain.driveArcade(0, 0);
	}

	@Override
	public void start() {
		Scheduler.add(this);
	}
}