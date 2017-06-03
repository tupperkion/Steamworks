package com.midcoastmaneiacs.Steamworks.auto;

import com.midcoastmaneiacs.Steamworks.Robot;
import com.midcoastmaneiacs.Steamworks.Scheduler;
import edu.wpi.first.wpilibj.command.CommandGroup;

public class Auto extends MMCommand {
	/** modes: 0 = gear, 1 = surge, 2 = play dead */
	byte mode;
	public Auto(byte mode) {
		this.mode = mode;
	}

	@Override
	protected void initialize() {
		Robot.driveTrain.takeControl(this);
		switch(mode) {
			case(0):
				switch (Robot.starting) {
					case (1):
					case (3):
						(new Series((new DriveCommand(-0.5, 0.5)),
									  (new DriveCommand(0, 0)),
									  (new Gear(true)))).start();
						break;
					case (2):
					default:
						(new Gear(true)).start();
						break;
				}
				break;
			case(1):
				(new DriveCommand(-0.6, 2)).start();
				break;
			default:
				break;
		}
	}

	@Override
	protected void end() {
		Robot.driveTrain.relinquishControl(this);
	}

	@Override
	protected boolean isFinished() {
		return !Robot.driveTrain.controlledBy(this) || super.isFinished();
	}
}
