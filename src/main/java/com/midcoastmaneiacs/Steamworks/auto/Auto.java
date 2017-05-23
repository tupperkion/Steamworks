package com.midcoastmaneiacs.Steamworks.auto;

import com.midcoastmaneiacs.Steamworks.Robot;

import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.command.CommandGroup;

/**
 *
 */
public class Auto extends CommandGroup {
	public Command selectedCommand = null;

	// modes: 0 = gear, 1 = surge, 2 = play dead
	public Auto(byte mode) {
		switch(mode) {
			case(0):
				switch (Robot.starting) {
					case (1):
					case (3):
						addSequential(new Routine("Drive:-0.5:0.5;Stop;Gear"));
						addSequential(new DriveCommand(0.4, 0.25));
						break;
					case (2):
					default:
						addSequential(new Gear(true));
						addSequential(new DriveCommand(0.4, 0.25));
						break;
				}
				break;
			case(1):
				addSequential(new DriveCommand(-0.6, 2));
				break;
			default:
				break;
		}
	}

	@Override
	protected void initialize() {
		Robot.driveTrain.takeControl(this);
	}

	@Override
	protected void end() {
		Robot.driveTrain.relinguishControl(this);
	}

	@Override
	protected boolean isFinished() {
		return !Robot.driveTrain.controlledBy(this) || super.isFinished();
	}
}
