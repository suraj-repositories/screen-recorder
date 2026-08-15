package com.oranbyte.screenrec.share.localsend;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.json.JSONObject;

public class LocalSendDiscovery {

	private final int port;

	private final Map<String, LocalSendDevice> devices = new ConcurrentHashMap<>();

	private final List<MulticastSocket> sockets = new ArrayList<>();

	private volatile boolean running;

	public LocalSendDiscovery() {
		this(LocalSendProtocol.DEFAULT_PORT);
	}

	public LocalSendDiscovery(int port) {
		this.port = port;
	}

	public synchronized void start() {

		if (running) {
			return;
		}

		running = true;

		startMulticastListeners();

		startAnnouncementThread();
	}

	public synchronized void stop() {

		running = false;

		synchronized (sockets) {

			for (MulticastSocket socket : sockets) {

				try {
					socket.close();
				} catch (Exception ignored) {
				}
			}

			sockets.clear();
		}

		devices.clear();
	}

	public List<LocalSendDevice> getDevices() {

		return Collections.unmodifiableList(new ArrayList<>(devices.values()));
	}

	private void startMulticastListeners() {

		try {

			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

			while (interfaces.hasMoreElements()) {

				NetworkInterface networkInterface = interfaces.nextElement();

				if (!isUsableInterface(networkInterface)) {

					continue;
				}

				try {

					MulticastSocket socket = new MulticastSocket(port);

					socket.setReuseAddress(true);

					socket.setOption(StandardSocketOptions.SO_REUSEADDR, true);

					InetAddress group = InetAddress.getByName(LocalSendProtocol.MULTICAST_ADDRESS);

					socket.joinGroup(new InetSocketAddress(group, port), networkInterface);

					synchronized (sockets) {
						sockets.add(socket);
					}

					Thread thread = new Thread(() -> listen(socket),
							"LocalSend-Multicast-" + networkInterface.getName());

					thread.setDaemon(true);
					thread.start();

				} catch (Exception e) {

					System.err.println("Unable to listen on " + networkInterface.getName() + ": " + e.getMessage());
				}
			}

		} catch (SocketException e) {

			System.err.println("Unable to enumerate network " + "interfaces: " + e.getMessage());
		}
	}

	private boolean isUsableInterface(NetworkInterface networkInterface) throws SocketException {

		return networkInterface.isUp() && !networkInterface.isLoopback() && networkInterface.supportsMulticast();
	}

	private void listen(MulticastSocket socket) {

		byte[] buffer = new byte[8192];

		while (running && !socket.isClosed()) {

			try {

				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

				socket.receive(packet);

				processPacket(packet);

			} catch (IOException e) {

				if (running) {

					System.err.println("Discovery receive error: " + e.getMessage());
				}

				return;
			}
		}
	}

	private void processPacket(DatagramPacket packet) {

		try {

			String body = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);

			JSONObject json = new JSONObject(body);

			String fingerprint = json.optString("fingerprint", "");

			if (LocalSendIdentity.getFingerprint().equals(fingerprint)) {

				return;
			}

			boolean announce = json.optBoolean("announce", false);

			String address = packet.getAddress().getHostAddress();

			if (announce) {

				sendMulticastResponse(packet.getAddress());

			} else {

				addDevice(address, json);
			}

		} catch (Exception ignored) {
			// Ignore invalid discovery packets.
		}
	}

	private void addDevice(String address, JSONObject json) {

		LocalSendDevice device = LocalSendDevice.fromJson(address, json);

		if (!device.getFingerprint().isBlank()) {

			devices.put(device.getFingerprint(), device);
		}
	}

	private void sendMulticastResponse(InetAddress destination) {

		JSONObject json = createDeviceJson();

		json.put("announce", false);

		byte[] data = json.toString().getBytes(StandardCharsets.UTF_8);

		try {

			synchronized (sockets) {

				for (MulticastSocket socket : sockets) {

					DatagramPacket packet = new DatagramPacket(data, data.length, destination, port);

					socket.send(packet);
				}
			}

		} catch (IOException e) {

			System.err.println("Unable to send discovery response: " + e.getMessage());
		}
	}

	private void startAnnouncementThread() {

		Thread thread = new Thread(() -> {

			while (running) {

				announce();

				try {

					Thread.sleep(5000);

				} catch (InterruptedException e) {

					Thread.currentThread().interrupt();

					return;
				}
			}
		}, "LocalSend-Announcer");

		thread.setDaemon(true);
		thread.start();
	}

	private void announce() {

		JSONObject json = createDeviceJson();

		json.put("announce", true);

		byte[] data = json.toString().getBytes(StandardCharsets.UTF_8);

		try {

			InetAddress group = InetAddress.getByName(LocalSendProtocol.MULTICAST_ADDRESS);

			synchronized (sockets) {

				for (MulticastSocket socket : sockets) {

					DatagramPacket packet = new DatagramPacket(data, data.length, group, port);

					socket.send(packet);
				}
			}

		} catch (IOException e) {

			if (running) {

				System.err.println("Unable to send LocalSend " + "announcement: " + e.getMessage());
			}
		}
	}

	private JSONObject createDeviceJson() {

		JSONObject json = new JSONObject();

		json.put("alias", LocalSendIdentity.getAlias());

		json.put("version", LocalSendIdentity.getVersion());

		json.put("deviceModel", LocalSendIdentity.getDeviceModel());

		json.put("deviceType", LocalSendIdentity.getDeviceType());

		json.put("fingerprint", LocalSendIdentity.getFingerprint());

		json.put("port", port);

		json.put("protocol", LocalSendProtocol.PROTOCOL_HTTPS);

		json.put("download", false);

		return json;
	}
}