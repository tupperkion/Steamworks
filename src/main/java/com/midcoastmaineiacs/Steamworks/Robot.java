package com.midcoastmaineiacs.Steamworks;

import com.midcoastmaineiacs.Steamworks.auto.*;
import edu.wpi.first.wpilibj.*;
import edu.wpi.first.wpilibj.command.Command;
import edu.wpi.first.wpilibj.livewindow.LiveWindow;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;

@SuppressWarnings("WeakerAccess")
public class Robot extends IterativeRobot {
	/** How long is the end game period (in seconds)? Currently used to notify the driver */
	public static final int ENDGAME = 30;

	/** ONLY FOR TESTING PURPOSES */
	public static final boolean FORCE_COMPETITION = false;
	/** If true, competition mode will be enabled when practice mode is enabled */
	public static final boolean PRACTICE_IS_COMPETITION = true;
	/** will update based on FMS status, updates on robot init, and when teleop and auto modes are entered */
	public static boolean competition = false;
	/** if true, enable values being displayed on the smartdashboard */
	public static final boolean ENABLE_SMARTDASHBOARD = false;
	/** if true and ENABLE_SMARTDASHBOARD is true, use the smartdashboard instead of our dashboard for retrieving values */
	public static final boolean PREFER_SMARTDASHBOARD = false;

	// 1 = left; 2 = center; 3 = right
	public static byte starting = 2;
	/** Starting position on field */
	public static SendableChooser<Byte> pos;
	/** Chosen auto routine */
	public static SendableChooser<Command> autochooser;
	public static VisionServer vision;
	public static Thread visionThread;

	public static DriveTrain driveTrain;
	public static Climber climber;
	public static List<MMSubsystem> subsystems;

	public static final Joystick joystick = new Joystick(0);

	public static Timer clock;
	public static Thread mainThread;
	public static DashboardServer dashboard;

	@Override
	public void robotInit() {
		mainThread = Thread.currentThread();

		driveTrain = new DriveTrain();
		climber = new Climber();
		subsystems = new ArrayList<>();
		subsystems.add(driveTrain);
		subsystems.add(climber);

		driveTrain.gyro.calibrate();

		dashboard = new DashboardServer();

		if (FORCE_COMPETITION) { // FORCE_COMPETITION should always be off except when testing competition-only features!
			DriverStation.reportWarning("Force-competition mode is activated, MAKE SURE A PROGRAMMER KNOWS ABOUT THIS!", false);
			competition = true;
		}

		//Vision.init();
		vision = new VisionServer();
		visionThread = new Thread(vision);
		visionThread.start();
		// front camera (rope climber end)
		CameraServer.getInstance().startAutomaticCapture(0).setExposureManual(40);
		// back camera (gear end)
		CameraServer.getInstance().startAutomaticCapture(1).setExposureManual(50);
		starting = (byte) DriverStation.getInstance().getLocation();

		if (ENABLE_SMARTDASHBOARD) {
			LiveWindow.addActuator("DriveTrain", "Left", (VictorSP) driveTrain.left);
			LiveWindow.addActuator("DriveTrain", "Right", (VictorSP) driveTrain.right);
			LiveWindow.addActuator("Climber", "Climber", (Spark) climber.climber);
			LiveWindow.addSensor("DriveTrain", "Gyro", driveTrain.gyro);
			if (PREFER_SMARTDASHBOARD) {
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
				autochooser.addObject("Gear", new Auto(Auto.Mode.GEAR));
				autochooser.addObject("Mobility", new Auto(Auto.Mode.SURGE));
				autochooser.addDefault("Play dead", new Auto(Auto.Mode.PLAY_DEAD));
				SmartDashboard.putData("Position", pos);
				SmartDashboard.putData("Auto", autochooser);
			}
		}

		if (!FORCE_COMPETITION) {
			//                              detect whether or not we're at a competition
			boolean willEnableCompetition = DriverStation.getInstance().isFMSAttached() || PRACTICE_IS_COMPETITION &&
																							   // detect practice mode
																							   DriverStation.getInstance().getMatchTime() > 0.0;
			if (!competition && willEnableCompetition)
				DriverStation.reportWarning("Competition mode activated", false);
			competition = willEnableCompetition;
		}

		// good to go, start the scheduler
		clock = new Timer(true);
		clock.scheduleAtFixedRate(new Scheduler(), 0, 20);
		System.out.println("All systems go!");
	}

	/** time / 20ms since last frame from rPi */
	public static int time = 0;

	/**
	 * Updates smartdashboard values, updates competition mode status, and verifies status of rPi
	 */
	@Override
	public void robotPeriodic() {
		if (joystick.getRawButton(7)) {
			Scheduler.enabled = false;
			if (Scheduler.teleop)
				Scheduler.enableTeleop(false);
		} else if (joystick.getRawButton(8) && !isDisabled()) {
			Scheduler.enabled = true;
			if (isOperatorControl())
				Scheduler.enableTeleop(true);
		}

		if (!rbWasPressed && joystick.getRawButton(5)) {
			rbWasPressed = true;
			//Vision.requestCapture();
			vision.requestCapture();
			notifyDriver();
		} else if (rbWasPressed && !joystick.getRawButton(5))
			rbWasPressed = false;

		if (ENABLE_SMARTDASHBOARD) {
			SmartDashboard.putBoolean("Competition mode", competition);
			SmartDashboard.putBoolean("Enabled", Scheduler.enabled);

			// report DriveTrain state
			SmartDashboard.putBoolean(" Disabled", driveTrain.getState() == DriveTrain.State.DISABLED);
			SmartDashboard.putBoolean(" Teleop", driveTrain.getState() == DriveTrain.State.TELEOP);
			SmartDashboard.putBoolean(" Command", driveTrain.getState() == DriveTrain.State.COMMAND);
			SmartDashboard.putBoolean(" Autopilot", driveTrain.getState() == DriveTrain.State.AUTOPILOT);

			if (PREFER_SMARTDASHBOARD)
				// update selectors and pray the DS is still alive to make these choices...
				starting = pos.getSelected();
			else
				starting = (byte) dashboard.getDouble("pos");
			// push gyro data in case camera mount falls (also useful for debugging)
			// normally gyro data is inverted as robot starts backwards when powered on, so the "+ 180" flips it
			SmartDashboard.putNumber("Heading", (driveTrain.getGyro() + 180) % 360);
		} else
			starting = (byte) dashboard.getDouble("pos");

		// if Pi hasn't responded for a second, it's probably dead
		// Pi "responds" by setting "true" to "Pi" every time it processes a frame
		/*if (vision.hasRecentUpdate())
			time = 0;
		else
			time++;
		if (time > 50)
			vision.setDead();
		if (vision.izgud() && !vision.isAlive())
			// clearly the Pi isn't on to target the peg
			vision.setBlind();*/
		if (ENABLE_SMARTDASHBOARD) {
			// push vision data for ease of lining up and debugging
			SmartDashboard.putBoolean("Sight", vision.isAlive() && vision.izgud());
			SmartDashboard.putBoolean("Pi", vision.isAlive());
			if (vision.izgud()) {
				SmartDashboard.putNumber("Vision", vision.getTurningAngle());
				//Vision.getTurningAngle();
				SmartDashboard.putNumber("Distance", vision.getDistance());
				SmartDashboard.putNumber("Camera distance", vision.getCameraDistance());
				SmartDashboard.putNumber("Camera angle", Math.toDegrees(vision.getCameraAngle()));
			} else {
				SmartDashboard.putNumber("Vision", 0);
				SmartDashboard.putNumber("Distance", 0);
				SmartDashboard.putNumber("Camera distance", 0);
				SmartDashboard.putNumber("Camera angle", 0);
			}
			// report power setting
			SmartDashboard.putBoolean("Power", fullPower);
		}

		dashboard.setBoolean("pi", vision.isAlive());
		dashboard.setBoolean("vision", vision.izgud());
		dashboard.setBoolean("competition", competition);
		dashboard.setBoolean("power", fullPower);
		dashboard.setBoolean("enabled", Scheduler.enabled);
		dashboard.setDouble("heading", driveTrain.getGyroMod() - 180);

		// debug values
		dashboard.setDouble("calc angle", vision.getTurningAngle());
		dashboard.setDouble("calc distance", vision.getDistance());
		dashboard.setDouble("cam angle", vision.getCameraAngle());
		dashboard.setDouble("cam distance", vision.getCameraDistance());
		dashboard.setBoolean("endgame", endgamePassed);
	}

	@Override
	public void disabledInit() {
		Scheduler.enabled = false;
		Scheduler.enableTeleop(false);
		for (MMSubsystem i: subsystems)
			i.stop();
		if (competition)
			DriverStation.reportWarning("Competition mode activated, commands not cancelled. " +
											"If you are at a competition or practice match, this is normal. " +
											"Otherwise, tell a programmer.", false);
		else
			Scheduler.cancelAllCommands();
		if (ENABLE_SMARTDASHBOARD)
			SmartDashboard.putBoolean("Endgame", false);
		endgamePassed = false;
	}

	//////////
	// Auto //
	//////////

	public static Command auto;

	/**
	 * Picks auto routine from autochooser and starts it
	 */
	@Override
	public void autonomousInit() {
		if (!FORCE_COMPETITION) {
			//                              detect whether or not we're at a competition
			boolean willEnableCompetition = DriverStation.getInstance().isFMSAttached() || PRACTICE_IS_COMPETITION &&
																							   // detect practice mode
																							   DriverStation.getInstance().getMatchTime() > 0.0;
			if (!competition && willEnableCompetition)
				DriverStation.reportWarning("Competition mode activated", false);
			if (!FORCE_COMPETITION)
				competition = willEnableCompetition;
		}
		driveTrain.gyro.reset();
		Scheduler.enabled = true;
		Scheduler.enableTeleop(false);
		if (ENABLE_SMARTDASHBOARD && PREFER_SMARTDASHBOARD)
			auto = autochooser.getSelected();
		else
			auto = new Auto(dashboard.getString("auto").equals("gear") ? Auto.Mode.GEAR : dashboard.getString("auto").equals("mobility") ? Auto.Mode.SURGE : Auto.Mode.PLAY_DEAD);
		auto.start();
	}

	public static boolean killSwitch() {
		return joystick.getRawButton(1); // A
	}

	////////////
	// Teleop //
	////////////
	/*
	 * Current mappings:
	 *
	 * Sticks         Drive
	 * LB             Toggle speed (100% or 50%, reflected by "Power" light in SmartDashboard, green = 100%)
	 * A              Force control to be taken from auto routine in competition
	 * Y              Run auto routine (defined by SmartDashboard controls)
	 *
	 * POV            Drive arcade (temporarily disables tank drive and trigger-based climber controls)
	 * Right stick X  Additional turning control while using POV arcade drive
	 * Right trigger  Throttle for POV arcade drive
	 * Left trigger   Throttle for POV arcade drive, half speed
	 *
	 * RB             Climb (100%)
	 * B              Climb (50%, use when at top of touch pad to hold position, just tap the button repeatedly)
	 * Left trigger   Climb down
	 * Right trigger  Climb up
	 *
	 * Back           Disable robot
	 * Start          Enable robot (if driver station allows)
	 *
	 * The following will always be mapped, but aren't likely to be used during comp
	 *
	 * X  Reverse climber (50%, to be used during demonstrations and testing, not during comp)
	 *
	 * Notifiers:
	 *
	 * Endgame
	 * DriveTrain state change (from/to DISABLED or TELEOP)
	 */

	private static boolean lbWasPressed = false;
	private static boolean rbWasPressed = false;
	/** "true" = 100% power (competition mode), "false" = 50% power (demonstration/small space mode) */
	private static boolean fullPower = true;
	/** "true" adds a 15% dead zone in the middle of the controller to ensure joysticks rest in non-motor-moving position */
	private static final boolean DEAD_ZONE = true;
	/** true = teleop is enabled but the driver hasn't gotten control yet */
	private static boolean waitingForTeleop = true;
	/** Have we notified the driver of endgame since last disable? */
	private static boolean endgamePassed = false;

	@Override
	public void teleopInit() {
		if (!FORCE_COMPETITION) {
			//                              detect whether or not we're at a competition
			boolean willEnableCompetition = DriverStation.getInstance().isFMSAttached() || PRACTICE_IS_COMPETITION &&
																							   // detect practice mode
																							   DriverStation.getInstance().getMatchTime() > 0.0;
			if (!competition && willEnableCompetition)
				DriverStation.reportWarning("Competition mode activated", false);
			if (!FORCE_COMPETITION)
				competition = willEnableCompetition;
		}
		Scheduler.enabled = true;
		Scheduler.enableTeleop(true);
		waitingForTeleop = !driveTrain.controlledByTeleop();

		if (!competition) {
			// commands should've been cancelled during disabledInit, but just to be safe
			Scheduler.cancelAllCommands();
		}
		/*if (driveTrain.controlledByTeleop()) {
			notifyDriver();
		}*/

		/*SmartDashboard.putBoolean("Endgame", false);
		(new Timer()).schedule(new TimerTask() {
			@Override
			public void run() {
				// endgame has arrived
				if (Scheduler.enabled) {
					notifyDriver();
					SmartDashboard.putBoolean("Endgame", true);
				}
			}
			// using edu.wpilib.first.wpilibj.Timer is based on how long teleop has been enabled, not the match
			// configuration, so this will alert the driver at the right time even when not in practice or FMS mode
		}, (long) ((edu.wpi.first.wpilibj.Timer.getMatchTime() + 150 - ENDGAME) * 1000));*/
	}

	@Override
	public void teleopPeriodic() {
		// Endgame notification
		if (!endgamePassed && DriverStation.getInstance().getMatchTime() > 0 && DriverStation.getInstance().getMatchTime() <= ENDGAME) {
			if (ENABLE_SMARTDASHBOARD)
				SmartDashboard.putBoolean("Endgame", true);
			endgamePassed = true;
			notifyDriver();
		}

		if (!Scheduler.enabled) return;
		// DriveTrain control
		if (driveTrain.controlledByTeleop()) {
			waitingForTeleop = false;
			if (joystick.getPOV() != -1) { // TODO: test POV driving
				double forward = Math.cos(Math.toRadians(joystick.getPOV()));
				double turn = -Math.sin(Math.toRadians(joystick.getPOV())) + joystick.getRawAxis(4); // 4 = right X
				double throttle = joystick.getRawAxis(2) / 2 + joystick.getRawAxis(3);
				driveTrain.driveArcade(forward * throttle, turn * throttle);
				if (climber.controlledByTeleop())
					climber.stop();
			} else {
				// left axis = 1, right axis = 5
				double leftSpeed = -joystick.getRawAxis(1);
				double rightSpeed = -joystick.getRawAxis(5);
				//noinspection ConstantConditions
				driveTrain.driveCurved(!DEAD_ZONE || Math.abs(leftSpeed) > 0.15 ? leftSpeed * (fullPower ? 1 : 0.5) : 0,
					!DEAD_ZONE || Math.abs(rightSpeed) > 0.15 ? rightSpeed * (fullPower ? 1 : 0.5) : 0);
			}
			if (joystick.getRawButton(4)) {
				//autochooser.getSelected().start();
				MMCommand command = new Gear(Gear.ScanMode.STATION);
				driveTrain.takeControl(command);
				command.start();
			}
		} else

		// Auto take-over
		if (waitingForTeleop /*|| killSwitch() should be handled by commands themselves */) {
			// this means that the auto period has just ended, teleop has just started, but the auto routine is still
			// running, waiting for the driver to manually take control, or the driver is holding down A, so autonomous
			// commands started during teleop need to be killed.
			if (Math.abs(joystick.getRawAxis(1)) >= 0.9 && Math.abs(joystick.getRawAxis(5)) >= 0.9 /*|| killSwitch()*/) {
				Scheduler.cancelAllCommands();
				waitingForTeleop = false;
			}
		}

		// Climber control
		if (climber.controlledByTeleop()) {
			if (joystick.getRawButton(6)) // RB
				climber.set(1);
			else if (joystick.getRawButton(2)) // B
				climber.set(0.5);
			else if (joystick.getRawButton(3)) // X
				climber.set(-0.5);
			else if (joystick.getPOV() == -1)
				climber.set(joystick.getRawAxis(3) - joystick.getRawAxis(2));
		}
	}

	@Override
	public void disabledPeriodic() {
		if (!lbWasPressed && joystick.getRawButton(5)) { // LB
			lbWasPressed = true;
			fullPower = !fullPower;
		} else if (lbWasPressed && !joystick.getRawButton(5)) {
			lbWasPressed = false;
		}
		if (killSwitch())
			Scheduler.cancelAllCommands();
	}

	/**
	 * Notifies the driver of some sort of event (by rumbling the controller).
	 */
	public static void notifyDriver() {
		(new Notifier()).start();
	}

	@Override
	public void testPeriodic() {
		LiveWindow.run();
	}
	@Override
	public void autonomousPeriodic() {}
}
