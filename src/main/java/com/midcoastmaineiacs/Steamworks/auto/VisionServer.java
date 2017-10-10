package com.midcoastmaineiacs.Steamworks.auto;

import com.midcoastmaineiacs.Steamworks.WebSocketTableServer;
import org.java_websocket.WebSocket;

public class VisionServer extends WebSocketTableServer {
	private static final int PORT = 5506;
	/** horizontal FOV of camera */
	private static final double HORIZONTAL_FOV = Math.toRadians(62.2d);
	private static final int CAMERA_WIDTH = 480;
	/** distance from middle of bot to camera */
	private static final double c = 11.5d;

	public VisionServer() {
		super("VisionServer", PORT, true);
	}

	@Override
	protected void setDefaults() {
		setBoolean("izgud", false);
		setBoolean("capture", false);
		setDouble("distance", 0);
		setDouble("error", 0);
	}

	public boolean isAlive() {
		return sockets.size() > 0;
	}

	public void setBlind() {
		setBoolean("izgud", false);
	}

	public boolean izgud() {
		return getBoolean("izgud");
	}

	public double getCameraDistance() {
		return getDouble("distance");
	}

	@SuppressWarnings("WeakerAccess")
	public double getError() {
		return getDouble("distance");
	}

	public double getCameraAngle() {
		return getError() / CAMERA_WIDTH * HORIZONTAL_FOV;
	}

	/**
	 * Uses the Law of Cosines to calculate the distance from the gear (middle of robot) to the peg.
	 */
	public double getDistance() {
		double a = getCameraDistance();
		double B = Math.PI / 2 + getCameraAngle();
		return Math.sqrt((a * a) + (c * c) - (2 * a * c * Math.cos(B)));
	}

	/**
	 * Calculates turning angle. Provide "distance" to skip calculations. This is the exact angle that the robot needs
	 * to turn. Uses the Law of Sines.
	 *
	 * @param distance the distance from the robot to the tape ({@link VisionServer#getDistance()})
	 */
	public double getTurningAngle(double distance) {
		double a = getCameraDistance();
		double B = Math.PI / 2 + getCameraAngle();
		@SuppressWarnings("UnnecessaryLocalVariable")
		double b = distance;
		if (a <= c)
			return 90 - Math.toDegrees(Math.asin(Math.sin(B) / b * a));
		return 90 - 180 + Math.toDegrees(B + Math.asin(Math.sin(B) / b * c));
	}

	/**
	 * Calculates turning angle. Calculates absolute distance for convenience.
	 * @see VisionServer#getTurningAngle(double distance)
	 */
	public double getTurningAngle() {
		return getTurningAngle(getDistance());
	}

	public void requestCapture() {
		setBoolean("capture", true);
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		super.onClose(conn, code, reason, remote);
		if (sockets.size() == 0)
			setBoolean("izgud", false);
	}
}
