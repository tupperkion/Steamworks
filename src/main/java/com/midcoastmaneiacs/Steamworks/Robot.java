package com.midcoastmaneiacs.Steamworks;

import com.midcoastmaneiacs.Steamworks.auto.Auto;
import com.midcoastmaneiacs.Steamworks.auto.Vision;
import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

public class Robot extends IterativeRobot {
	public Command auto;

	/** ONLY FOR TESTING PURPOSES */
	public static final boolean forceCompetition = false;
	/** will update based on FMS status */
	public static boolean competition = false;

	// 1 = left; 2 = center; 3 = right
	public static byte starting = 1;
	/** Starting position on field */
	public static SendableChooser<Byte> pos;
	/** Chosen auto routine */
	public static SendableChooser<Command> autochooser;
	/** Some drivers may prefer rumble to be off, hence the toggle */
	public static SendableChooser<Boolean> rumble;

	public static DriveTrain driveTrain;
	public static Climber climber;
	public static List<MMSubsystem> subsystems;

	public static Joystick joystick = new Joystick(0);

	public static Timer clock;

	public void robotInit() {
		subsystems = new ArrayList<>();
		driveTrain = new DriveTrain();
		climber = new Climber();
		subsystems.add(driveTrain);
		subsystems.add(climber);

		if (forceCompetition) { // forceCompetition should always be off except when testing competition-only features!
			System.err.println("WARNING: Force-competition mode is activated, MAKE SURE A PROGRAMMER KNOWS ABOUT THIS!");
			competition = true;
		}

		LiveWindow.addActuator("DriveTrain", "Left", (VictorSP) driveTrain.left);
		LiveWindow.addActuator("DriveTrain", "Right", (VictorSP) driveTrain.right);
		LiveWindow.addActuator("Climber", "Climber", (Spark) climber.climber);
		LiveWindow.addSensor("DriveTrain", "Gyro", driveTrain.gyro);

		Vision.init();
		// front camera (rope climber end)
		CameraServer.getInstance().startAutomaticCapture(0).setExposureManual(40);
		// back camera (gear end)
		CameraServer.getInstance().startAutomaticCapture(1).setExposureManual(50);
		driveTrain.gyro.calibrate();
		starting = (byte) DriverStation.getInstance().getLocation();
		pos = new SendableChooser<>();
		switch (starting) {
			case (1):
				pos.addDefault("Left", (byte) 1);
				pos.addObject("Center", (byte) 2);
				pos.addObject("Right", (byte) 3);
				break;
			case (3):
				pos.addObject("Left", (byte) 1);
				pos.addObject("Center", (byte) 2);
				pos.addDefault("Right", (byte) 3);
				break;
			case (2):
			default: // 'Center' is a safe bet.... right?
				pos.addObject("Left", (byte) 1);
				pos.addDefault("Center", (byte) 2);
				pos.addObject("Right", (byte) 3);
				break;
		}
		autochooser = new SendableChooser<>();
		autochooser.addObject("Gear", new Auto((byte) 0));
		autochooser.addObject("Mobility", new Auto((byte) 1));
		autochooser.addDefault("Play dead", new Auto((byte) 1)); // at least safe reset the conveyer
		rumble = new SendableChooser<>();
		rumble.addDefault("Rumble On", true);
		rumble.addObject("Rumble Off", false);
		SmartDashboard.putData("Rumble", rumble);
		SmartDashboard.putData("Position", pos);
		SmartDashboard.putData("Auto", autochooser);

		// good to go, start the scheduler
		clock = new Timer(true);
		clock.scheduleAtFixedRate(new Scheduler(), 0, 20);
		System.out.println("All systems go!");
	}

	public void disabledInit() {
		Scheduler.enabled = false;
		for (MMSubsystem i: subsystems)
			i.stop();
		if (!competition)
			Scheduler.cancelAllCommands();
	}

	public void disabledPeriodic() {
		//edu.wpi.first.wpilibj.command.Scheduler.getInstance().run();
	}

	public void autonomousPeriodic() {
		//edu.wpi.first.wpilibj.command.Scheduler.getInstance().run();
	}

	public void teleopInit() {
		Scheduler.enabled = true;
		driveTrain.enableTeleop(true);
		climber.enableTeleop(true);

		if (!competition) {
			// commands should've been cancelled during disabledInit, but just to be safe
			Scheduler.cancelAllCommands();
			driveTrain.takeControl(null);
		}
		if (driveTrain.controlledByTeleop()) {
			notifyDriver();
		}
	}

	/**
	 * This function is called periodically during test mode
	 */
	public void testPeriodic() {
		LiveWindow.run();
	}

	/**
	 * Picks auto routine from autochooser and starts it
	 */
	public void autonomousInit() {
		Scheduler.enabled = true;
		auto = autochooser.getSelected();
		starting = pos.getSelected();
		if (auto != null)
			auto.start();
	}

	/** time / 20ms since last frame from rPi */
	public static int time = 0;

	/**
	 * Updates smartdashboard values, updates competition mode status, and verifies status of rPi
	 */
	public void robotPeriodic() {
		// detect whether or not we're at a competition
		if (!competition && DriverStation.getInstance().isFMSAttached())
			System.err.println("INFO: Competition mode activated");
		if (!forceCompetition)
			competition = DriverStation.getInstance().isFMSAttached();
		SmartDashboard.putBoolean("Competition mode", competition);

		// if Pi hasn't responded for a second, it's probably dead
		// Pi "responds" by setting "true" to "Pi" every time it processes a frame
		SmartDashboard.putBoolean("Pi", time < 50);
		if (Vision.table.getBoolean("running", false)) {
			time = 0;
			Vision.table.putBoolean("running", false);
		} else
			time++;
		if (Vision.izgud() && !Vision.isalive()) {
			// clearly the Pi isn't on to target the peg
			Vision.table.putBoolean("sight", false);
		}
		SmartDashboard.putBoolean("Sight", Vision.izgud());

		// update selectors and pray the DS is still alive to make these choices...
		starting = pos.getSelected();

		// push gyro data in case camera mount falls (also useful for debugging)
		// normally gyro data is inverted as robot starts backwards when powered on, so the "+ 180" flips it
		SmartDashboard.putNumber("Heading", (driveTrain.getGyro() + 180) % 360);
		// push vision data for ease of lining up and debugging
		if (Vision.izgud()) {
			SmartDashboard.putNumber("Vision", Vision.getTurningAngle());
			//Vision.getTurningAngle();
			SmartDashboard.putNumber("Distance", Vision.getDistance());
			SmartDashboard.putNumber("Camera distance", Vision.getCameraDistance());
			SmartDashboard.putNumber("Camera angle", Math.toDegrees(Vision.getCameraAngle()));
		} else {
			SmartDashboard.putNumber("Vision", 0);
			SmartDashboard.putNumber("Distance", 0);
			SmartDashboard.putNumber("Camera distance", 0);
			SmartDashboard.putNumber("Camera angle", 0);
		}
		// report power setting
		SmartDashboard.putBoolean("Power", fullPower);
	}

	////////////
	// Teleop //
	////////////

	private boolean lbWasPressed = false;
	/** "true" = 100% power (competition mode), "false" = 50% power (demonstration/small space mode) */
	private boolean fullPower = true;
	/** "true" adds a 15% dead zone in the middle of the controller to ensure joysticks rest in non-motor-moving position */
	private boolean deadZone = true;

	/*
	 * Current mappings:
	 *
	 * Sticks         Drive
	 * LB             Toggle speed (100% or 50%, reflected by "Power" light in SmartDashboard, green = 100%)
	 * A              Force control to be taken from auto routine in competition
	 *
	 * POV            Drive arcade (temporarily disables tank drive and trigger-based climber controls)
	 * Right stick X  Additional turning control while using POV arcade drive
	 * Right trigger  Throttle for POV arcade drive
	 *
	 * RB        Climb (100%)
	 * B         Climb (50%, use when at top of touch pad to hold position, just tap the button repeatedly)
	 * Triggers  Alternative controls for climbing (not sure which is which yet, the NI DriverStation is weird)
	 *
	 * The following will always be mapped, but aren't likely to be used during comp
	 *
	 * A  Kill routines (may become useful if routines become used during teleop)
	 * Y  Auto gear (primarily for testing purposes,
	 *        can be used during comp if "Pi" and "Sight" on SmartDashboard are BOTH green)
	 * X  Reverse climber (50%, to be used during demonstrations and testing, not during comp)
	 */
	public void teleopPeriodic() {
		//edu.wpi.first.wpilibj.command.Scheduler.getInstance().run(); // just in case a Command is running...

		// speed toggle
		if (!lbWasPressed && joystick.getRawButton(5)) { // LB
			lbWasPressed = true;
			fullPower = !fullPower;
		} else if (lbWasPressed && !joystick.getRawButton(5)) {
			lbWasPressed = false;
		}

		if (driveTrain.controlledByTeleop()) {
			if (joystick.getPOV() != -1) {
				double forward = Math.cos(Math.toRadians(joystick.getPOV()));
				double turn = Math.sin(Math.toRadians(joystick.getPOV())) + joystick.getRawAxis(4); // 4 = right X
				driveTrain.driveArcade(forward * -joystick.getZ(), turn * -joystick.getZ());
			} else {
				// left axis = 1, right axis = 5
				double leftSpeed = -joystick.getRawAxis(1);
				double rightSpeed = -joystick.getRawAxis(5);
				driveTrain.driveLeftCurved(!deadZone || Math.abs(leftSpeed) > 0.15 ? leftSpeed * (fullPower ? 1 : 0.5) : 0);
				driveTrain
					.driveRightCurved(!deadZone || Math.abs(rightSpeed) > 0.15 ? rightSpeed * (fullPower ? 1 : 0.5) : 0);
			}
		} else {
			// this means that the auto period has just ended, teleop has just started, but the auto routine is still
			// running, waiting for the driver to manually take control
			if (Math.abs(joystick.getRawAxis(1)) >= 0.9 && Math.abs(joystick.getRawAxis(5)) >= 0.9 ||joystick.getRawButton(1)) {
				Scheduler.cancelAllCommands();
				driveTrain.enableTeleop(true);
				driveTrain.takeControl(null); // ensure teleop has control
				notifyDriver();
			}
		}

		if (climber.controlledByTeleop()) {
			if (joystick.getRawButton(6)) // RB
				climber.set(1);
			else if (joystick.getRawButton(2)) // B
				climber.set(0.5);
			else if (joystick.getRawButton(3)) // X
				climber.set(-0.5);
			else if (joystick.getPOV() == -1)
				climber.set(-joystick.getZ());
		}
	}

	/**
	 * Notifies the driver of some sort of event (by rumbling the controller).
	 */
	public static void notifyDriver() {
		(new Notifier()).start();
	}
}
