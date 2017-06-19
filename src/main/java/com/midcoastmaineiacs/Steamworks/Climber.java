package com.midcoastmaineiacs.Steamworks;

import edu.wpi.first.wpilibj.Spark;
import edu.wpi.first.wpilibj.SpeedController;

public class Climber extends MMSubsystem {
	public final SpeedController climber = new Spark(3);

	public void set(double speed) {
		if (verifyResponse())
			climber.set(speed);
	}

	@Override
	public void stop() {
		climber.set(0);
	}
}
