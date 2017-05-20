package org.usfirst.frc5506.Steamworks.auto;

import org.usfirst.frc5506.Steamworks.Robot;

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
						addSequential(new Routine("Drive:-0.5;0.5;Stop;Gear"));
						addSequential(new Surge(true));
						break;
					case (2):
					default:
						addSequential(new Gear(true));
						addSequential(new Surge(true));
						break;
				}
				break;
			case(1):
				addSequential(new Surge());
				break;
			default:
				break;
		}
	}
}
