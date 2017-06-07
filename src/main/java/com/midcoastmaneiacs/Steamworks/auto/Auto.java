package com.midcoastmaneiacs.Steamworks.auto;

import com.midcoastmaneiacs.Steamworks.Robot;

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
		Robot.driveTrain.takeControl(this);
		switch(mode) {
			case GEAR:
				switch (Robot.starting) {
					case (1):
					case (3):
						(new Series((new DriveCommand(-0.5, 0.5)), (new Gear(true)))).start();
						break;
					case (2):
					default:
						(new Gear(true)).start();
						break;
				}
				break;
			case SURGE:
				if (Robot.starting == 2)
					System.err.println("WARNING: Surge cancelled due to illegal starting position!");
				else
					(new DriveCommand(-0.6, 2)).start();
				break;
			default:
				break;
		}
	}

	@Override
	protected boolean isFinished() {
		return !Robot.driveTrain.controlledBy(this) || super.isFinished();
	}
}
