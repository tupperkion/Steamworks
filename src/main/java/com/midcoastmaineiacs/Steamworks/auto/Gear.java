package com.midcoastmaineiacs.Steamworks.auto;

import com.midcoastmaineiacs.Steamworks.DriveTrain;
import com.midcoastmaineiacs.Steamworks.Robot;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.command.Command;

@SuppressWarnings("WeakerAccess")
public class Gear extends MMCommand {
	public final ScanMode scan;
	// have I found the vision targets?
	public boolean found;

	/**
	 * A {@link Command} which:
	 * <ul><li>
	 *     takes control of the {@link DriveTrain DriveTrain}
	 * </li><li>
	 *     scans by gradually pivoting left or right (see {@link Gear#scan}) until the peg is spotted by the camera
	 *     ({@link VisionServer#izgud()} returns {@code true})
	 * </li><li>
	 *     moves the robot to the peg, turning as necessary to maintain alignment
	 * </li><li>
	 *     backs away slightly from peg in order to allow gear to be taken out
	 * </li>
	 *
	 * <p>If {@code scan} is {@code false}, the command will <em>not</em> scan for the peg, and will immediately cancel
	 * if the peg is not currently in the camera's FOV.
	 *
	 * @param scan Whether or not to scan
	 */
	public Gear(ScanMode scan) {
		if (scan != ScanMode.NONE && !Robot.vision.isAlive()) scan = ScanMode.NONE;
		this.scan = scan;
	}

	public void initialize() {
		Robot.driveTrain.takeControl(this);
		found = Robot.vision.izgud();
	}

	/**
	 * @return True = scan right, false = scan left
	 */
	private boolean getScanDirection() {
		switch (scan) {
			case STARTING:
				return Robot.pos.getSelected() == 1;
			case STATION:
			default:
				return DriverStation.getInstance().getLocation() == 1 || DriverStation.getInstance().getLocation() == 2
																			 && DriverStation.getInstance().getAlliance() == DriverStation.Alliance.Red;
		}
	}

	public void execute() {
		if (!found) found = Robot.vision.izgud();
		if (scan != ScanMode.NONE && !found) {
			if (getScanDirection())
				// probably on left side of peg, scan right
				Robot.driveTrain.driveBackwards(0.3, -0.3);
			else
				// probably on right side of peg, scan left
				Robot.driveTrain.driveBackwards(-0.3, 0.3);
		}
		if (found && !requireChildren) {
			if (Robot.vision.izgud()) {
				//double distance = Vision.getDistance();
				//if (distance > 12) {
				double error = Robot.vision.getTurningAngle();
				if (error < -30) {
					// > 30 degrees to the right, turn left
					Robot.driveTrain.driveBackwards(-0.3, 0.3);
				} else if (error < -5) {
					// 5-30 degrees to the right, turn left
					Robot.driveTrain.driveBackwards(0, 0.3);
				} else if (error < 5) {
					// + or - 5 degrees, tolerable, go straight
					Robot.driveTrain.drive(-0.3);
				} else if (error < 30) {
					// 5-30 degrees to the left, turn right
					Robot.driveTrain.driveBackwards(0.3, 0);
				} else {
					// > 30 degrees to the left, turn right
					Robot.driveTrain.driveBackwards(0.3, -0.3);
				}
				//} else {
				//	if (!timing) {
				//		setTimeout(0.5);
				//		timing = true;
				//	}
				//	Robot.driveTrain.driveArcade(0.4, 0);
				//}
			} else {
				// Note the !requireChildren condition above. This is important so that once this Series is started,
				// A) the loop above which tries to maintain heading on the peg stops and B) only one Series is spawned
				// When coding, be careful not to allow commands to keep spawning endlessly
				(new Series(new DriveCommand(-0.2, 1), new DriveCommand(0.2, 0.25))).start();
				releaseForChildren();
			}
		}
	}

	public boolean isFinished() {
		return super.isFinished() || !Robot.driveTrain.controlledBy(this) || !Robot.vision.isAlive() || scan == ScanMode.NONE && !found;
	}

	public enum ScanMode {
		NONE,
		STARTING,
		STATION
	}
}
