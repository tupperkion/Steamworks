package com.midcoastmaineiacs.Steamworks;

public class DashboardServer extends WebSocketTableServer {
	private static final int PORT = 5800;

	public DashboardServer() {
		super("DashboardServer", PORT);
	}

	@Override
	protected void setDefaults() {
		setBoolean("pi", false);
		setBoolean("vision", false);
		setBoolean("competition", false);
		setBoolean("power", true);
		setBoolean("enabled", false);
		setDouble("pos", 2);
		setString("auto", "playdead");
		setDouble("heading", 180);
		setString("name", "Jeffrey");
	}
}
