package com.midcoastmaneiacs.Steamworks.auto;

import com.midcoastmaneiacs.Steamworks.Robot;
import edu.wpi.first.wpilibj.networktables.NetworkTable;

public class Vision {
	public static NetworkTable table;

	/** horizontal FOV of camera */
	private static final double cameraFOV = Math.toRadians(62.2d);
	private static final int cameraWidth = 480;

	/** distance from middle of bot to camera */
	private static final double c = 8.5d;

	public static boolean inited = false;

	public static void init() {
		NetworkTable.initialize();
		table = NetworkTable.getTable("vision");
		inited = true;
	}

	/** is the vision code ready and running? */
	public static boolean isalive() {
		return (table != null) && Robot.time < 50;
	}

	/** can the camera see and ID the tape? */
	public static boolean izgud() {
		return (table != null) && table.getBoolean("izgud", false) && isalive();
	}

	public static double getCameraDistance() {
		return table.getNumber("distance", 0d);
	}

	public static double getError() {
		return table.getNumber("error", 0d);
	}

	public static double getCameraAngle() {
		return getError() / cameraWidth * cameraFOV;
	}

	/** distance from gear to robot */
	public static double getDistance() {
		double a = getCameraDistance();
		double B = Math.PI / 2 + getCameraAngle();
		return Math.sqrt((a * a) + (c * c) - (2 * a * c * Math.cos(B)));
	}

	/**
	 * Calculates turning angle. Provide "distance" to skip calculations. This is the exact angle that the robot needs
	 * to turn.
	 *
	 * @param distance
	 *            the distance from the robot to the tape (getDistance())
	 */
	public static double getTurningAngle(double distance) {
		double a = getCameraDistance();
		double B = Math.PI / 2 + getCameraAngle();
		//return Math.toDegrees(B);
		double b = distance;
		if (a <= c)
			return 90 - Math.toDegrees(Math.asin(Math.sin(B) / b * a));
		return 90 - 180 + Math.toDegrees(B + Math.asin(Math.sin(B) / b * c));
	}

	/**
	 * Calculates turning angle. Calculates absolute distance for convenience.
	 */
	public static double getTurningAngle() {
		return getTurningAngle(getDistance());
	}
}
/*
 *   C
 *   |\
 *   | \
 *   |  \
 *   \   \
 *    |   \
 *    |    \
 *    |     \
 *  b \      \ a
 *     |      \
 *     |       \
 *     |        \
 *     \_________\
 *     A    c     B
 *
 * A: Position of gear B: Position of camera C: Position of peg
 *
 * c: static, distance between camera and robot
 * a: detected by VT
 * b: calculated
 * B: detected by VT
 * A: calculated
 */
