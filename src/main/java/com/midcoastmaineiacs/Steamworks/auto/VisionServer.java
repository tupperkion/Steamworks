package com.midcoastmaineiacs.Steamworks.auto;

import edu.wpi.first.wpilibj.DriverStation;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class VisionServer implements Runnable {
	/** horizontal FOV of camera */
	private static final double HORIZONTAL_FOV = Math.toRadians(62.2d);
	private static final int CAMERA_WIDTH = 480;

	/** distance from middle of bot to camera */
	private static final double c = 11.5d;

	private final int port;
	private DataOutputStream outputStream = null;
	@SuppressWarnings("FieldCanBeLocal")
	private DataInputStream inputStream = null;
	@SuppressWarnings("FieldCanBeLocal")
	private ServerSocket serverSocket;

	private boolean running = false;
	private boolean izgud = false;
	private double distance = 0d;
	private double error = 0d;

	public boolean isAlive() {
		return running;
	}

	public boolean izgud() {
		if (!running) izgud = false;
		return izgud;
	}

	public boolean hasRecentUpdate() {
		if (running) {
			running = false;
			return true;
		}
		return false;
	}

	public double getCameraDistance() {
		return distance;
	}

	@SuppressWarnings("WeakerAccess")
	public double getError() {
		return error;
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
		if (outputStream != null)
			try {
				outputStream.writeUTF("capture");
			} catch (Exception e) {
				e.printStackTrace();
			}
	}

	public VisionServer(int port) {
		this.port = port;
	}

	@Override
	public void run() {
		try {
			serverSocket = new ServerSocket(port);
			Outer:
			//noinspection InfiniteLoopStatement
			while (true) {
				Socket socket = serverSocket.accept();
				socket.setKeepAlive(true);
				outputStream = new DataOutputStream(socket.getOutputStream());
				inputStream = new DataInputStream(socket.getInputStream());
				while (true) {
					try {
						String msg = inputStream.readUTF();
						if (msg.equalsIgnoreCase("izntgud")) {
							running = true;
							izgud = false;
						} else if (msg.equalsIgnoreCase("dead")) {
							running = false;
							izgud = false;
						} else if (msg.contains("izgud")) {
							String[] split = msg.split(";");
							if (split.length == 3) {
								distance = Double.parseDouble(split[1]);
								error = Double.parseDouble(split[2]);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						running = false;
						try {
							if (!socket.isClosed() && !socket.isConnected()) {
								socket.close();
								continue Outer;
							}
						} catch (Exception e2) {
							e2.printStackTrace();
						}
					}
					if (Thread.interrupted())
						throw new InterruptedException();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			DriverStation.reportError("Error in vision server: " + e, true);
			try {
				serverSocket.close();
			} catch (IOException e2) {
				e2.printStackTrace();
			}
		}
	}
}
