package com.midcoastmaineiacs.Steamworks.auto;

import com.midcoastmaineiacs.Steamworks.Robot;
import edu.wpi.first.wpilibj.DriverStation;

@SuppressWarnings("WeakerAccess")
public class Auto extends MMCommand {
	public enum Mode {
		PLAY_DEAD,
		SURGE,
		GEAR
	}

	public final Mode mode;
	public Auto(Mode mode) {
		this.mode = mode;
	}

	@Override
	protected void initialize() {
		System.out.println(Thread.currentThread());
		Robot.driveTrain.takeControl(this);
		switch(mode) {
			case GEAR:
				switch (Robot.starting) {
					case (1):
					case (3):
						(new Series((new DriveCommand(-0.5, 0.5)), (new Gear(Gear.ScanMode.STARTING)))).start();
						break;
					case (2):
					default:
						(new Gear(Gear.ScanMode.STARTING)).start();
						break;
				}
				break;
			case SURGE:
				if (Robot.starting == 2)
					DriverStation.reportError("Surge cancelled due to illegal starting position!", false);
				else
					(new DriveCommand(-0.2, 10)).start();//(new DriveCommand(-0.6, 2)).start();
				break;
			default:
				break;
		}
		releaseForChildren();
	}

	@Override
	protected boolean isFinished() {
		return !Robot.driveTrain.controlledBy(this) || super.isFinished();
	}
}
