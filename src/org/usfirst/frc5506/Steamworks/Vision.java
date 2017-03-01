package org.usfirst.frc5506.Steamworks;

import edu.wpi.first.wpilibj.networktables.NetworkTable;

public class Vision {
	public static NetworkTable table;
	
	// horizontal FOV of camera
	public static final double cameraFOV = Math.toRadians(62.2d);
	public static final int cameraWidth = 480;
	
	// distance from middle of bot to camera
	public static final double c = 8.5d;
	
	public static boolean inited = false;
	
	public static void init() {
		NetworkTable.initialize();
		table = NetworkTable.getTable("vision");
		inited = true;
	}
	
	// is the vision code ready and running?
	public static boolean isalive() {
		return table.getBoolean("running", false);
	}
	
	// can the camera see and ID the tape?
	public static boolean izgud() {
		return table.getBoolean("izgud", false);
	}
	
	public static double getCameraDistance() {
		return table.getNumber("distance", 0d);
	}
	
	public static double getError() {
		return table.getNumber("error", 0d);
	}
	
	public static double getCameraAngle() {
		return getError() / cameraWidth * cameraFOV + 0.5 * Math.PI;
	}
	
	// distance from gear to robot
	public static double getDistance() {
		double a = getCameraDistance();
		double B = getCameraAngle();
		return Math.sqrt(Math.pow(a, 2) + Math.pow(c, 2) + 2 * a * c * Math.cos(B));
	}
	
	/**
	 * Calculates turning angle. Provide "distance" to skip calculations.
	 * This is the exact angle that the robot needs to turn.
	 * 
	 * @param distance	the distance from the robot to the tape (getDistance())
	 */
	public static double getTurningAngle(double distance) {
		double a = getCameraDistance();
		double B = getCameraAngle();
		double b = distance;
		if (a <= c)
			return Math.toDegrees(Math.asin(a * (Math.sin(B) / b)));
		return 90 - Math.toDegrees(B - Math.asin(c * (Math.sin(B) / b)));
	}
	
	/**
	 * Calculates turning angle. Calculates absolute distance for convenience.
	 */
	public static double getTurningAngle() {
		return getTurningAngle(getDistance());
	}
}
/*

C
 |\
 | \
 |  \
 \   \
  |   \
  |    \
  |     \ a
  \      \
 b |      \
   |       \
   |        \
   \_________\
  A     c     B

A: Position of gear
B: Position of camera
C: Position of peg

c: static, distance between camera and robot
a: detected by VT
b: calculated

B: detected by VT
A: calculated
*/

