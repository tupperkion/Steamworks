package com.midcoastmaneiacs.Steamworks;

import edu.wpi.first.wpilibj.Spark;

import edu.wpi.first.wpilibj.SpeedController;
import edu.wpi.first.wpilibj.command.Subsystem;

public class Climber extends Subsystem {
	public final SpeedController climber = new Spark(3);

	public void set(double speed) {
		climber.set(speed);
	}

	public void initDefaultCommand() {}
}
