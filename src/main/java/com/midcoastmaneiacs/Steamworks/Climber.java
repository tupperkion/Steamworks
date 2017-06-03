package com.midcoastmaneiacs.Steamworks;

import edu.wpi.first.wpilibj.Spark;
import edu.wpi.first.wpilibj.SpeedController;

public class Climber extends MMSubsystem {
	public final SpeedController climber = new Spark(3);

	public void set(double speed) {
		climber.set(speed);
	}

	@Override
	public void stop() {
		set(0);
	}
}
