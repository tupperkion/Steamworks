package com.midcoastmaineiacs.Steamworks;

import com.sun.istack.internal.Nullable;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class WebSocketTableServer extends WebSocketServer {
	private final boolean recessive;
	private final String name;
	protected List<WebSocket> sockets = new ArrayList<>();
	private Map<String, Serializable> table = new HashMap<>();
	private boolean initialized = false;
	private Map<String, Serializable> defaults = new HashMap<>();
	private Map<String, Serializable> tempTable = null;

	public WebSocketTableServer(String name, int port) {
		this(name, port, false);
	}

	public WebSocketTableServer(String name, int port, boolean recessive) {
		super(new InetSocketAddress(port));
		this.name = name;
		this.recessive = recessive;
		setDefaults();
		initialized = true;
		start();
	}

	@Override
	public synchronized void onOpen(WebSocket conn, ClientHandshake handshake) {
		sockets.add(conn);
		System.out.println(name + ": New connection");
		if (recessive)
			conn.send("pull");
		else
			conn.send("sync:" + serializeAll(table));
	}

	@Override
	public synchronized void onClose(WebSocket conn, int code, String reason, boolean remote) {
		sockets.remove(conn);
		System.out.println(remote ? name + ": Connection closed by remote host: (" + code + ") " + reason : name + ": Connection closed.");
	}

	@Override
	public synchronized void onMessage(WebSocket conn, String message) {
		if (message.startsWith("sync:")) {
			Map<String, Serializable> oldTable = table;
			table = unserializeAll(message.substring(5));
			sendToOtherClients(message, conn);
			tempTable = oldTable;
			onSync(oldTable);
			tempTable = null;
		} else if (message.startsWith("change:")) {
			int i = message.indexOf(';');
			if (i == -1) return;
			int length = Integer.parseInt(message.substring(7, i));
			String key = message.substring(i + 1, i + length + 1);
			String value = message.substring(i + length + 1);
			if (!table.containsKey(key) || !table.get(key).equals(value)) {
				Serializable old = table.get(key);
				table.put(key, unserialize(value));
				sendToOtherClients(message, conn);
				onChange(key, old);
			}
		} else if (message.equals("pull"))
			synchronize();
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		System.err.println(name + ": Error with websocket " + conn);
		ex.printStackTrace();
	}

	@Override
	public void onStart() {
		System.out.println(name + ": Server started.");
	}

	private synchronized void sendToAllClients(String message) {
		for (WebSocket i: sockets) {
			i.send(message);
		}
	}

	private synchronized void sendToOtherClients(String message, WebSocket socket) {
		for (WebSocket i: sockets) {
			if (socket != i)
				i.send(message);
		}
	}

	private synchronized void setValue(String key, Serializable data) {
		if (initialized) {
			if (!table.containsKey(key) || !table.get(key).equals(data)) {
				Serializable old = table.get(key);
				table.put(key, data);
				sendToAllClients("change:" + key.length() + ";" + key + serialize(data));
			}
		} else {
			table.put(key, data);
			defaults.put(key, data);
		}
	}

	public void setString(String key, String value) {
		setValue(key, value);
	}

	public void setDouble(String key, double value) {
		setValue(key, value);
	}

	public void setBoolean(String key, boolean value) {
		setValue(key, value);
	}

	public String getString(String key) {
		return (String) table.getOrDefault(key, defaults.get(key));
	}

	public double getDouble(String key) {
		return (double) table.getOrDefault(key, defaults.get(key));
	}

	public boolean getBoolean(String key) {
		return (boolean) table.getOrDefault(key, defaults.get(key));
	}

	private static String serialize(Serializable s) {
		if (s instanceof Boolean)
			return "B" + s;
		else if (s instanceof Double)
			return "D" + s;
		else
			return "S" + s;
	}

	private static Serializable unserialize(String s) {
		if (s.startsWith("B"))
			return s.equalsIgnoreCase("Btrue");
		else if (s.startsWith("D"))
			return Double.parseDouble(s.substring(1));
		else
			return s.substring(1);
	}

	private synchronized static String serializeAll(Map<String, Serializable> map) {
		StringBuilder s = new StringBuilder();
		for (String i: map.keySet()) {
			String j = serialize(map.get(i));
			s.append(i.length());
			s.append(";");
			s.append(i);
			s.append(j.length());
			s.append(";");
			s.append(j);
		}
		return s.toString();
	}

	private synchronized static Map<String, Serializable> unserializeAll(String s) {
		Map<String, Serializable> map = new HashMap<>();
		int i = s.indexOf(';');
		while (i != -1) {
			int keyLength = Integer.parseInt(s.substring(0, i));
			String key = s.substring(i + 1, i + keyLength + 1);
			int i2 = s.indexOf(';', i + keyLength + 1);
			int valLength = Integer.parseInt(s.substring(i + keyLength + 1, i2));
			String val = s.substring(i2 + 1, i2 + valLength + 1);
			map.put(key, unserialize(val));
			s = s.substring(i2 + valLength + 1);
			i = s.indexOf(';');
		}
		return map;
	}

	public void synchronize() {
		sendToAllClients("sync:" + serializeAll(table));
	}

	protected abstract void setDefaults();

	protected void onSync(Map<String, Serializable> oldTable) {}

	protected void onChange(String key, @Nullable Serializable old) {}

	protected void rejectSync() {
		if (tempTable != null) {
			table = tempTable;
			tempTable = null;
			synchronize();
		}
	}
}
