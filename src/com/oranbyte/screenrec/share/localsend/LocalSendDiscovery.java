package com.oranbyte.screenrec.share.localsend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.json.JSONObject;

public class LocalSendDiscovery {

	private final int port;
	private final Consumer<LocalSendDevice> deviceListener;

	private final Map<String, LocalSendDevice> devices = new ConcurrentHashMap<>();
	private final List<MulticastSocket> sockets = new ArrayList<>();
	private volatile boolean running;

	public LocalSendDiscovery() {
		this(LocalSendProtocol.DEFAULT_PORT, null);
	}

	public LocalSendDiscovery(int port) {
		this(port, null);
	}

	public LocalSendDiscovery(Consumer<LocalSendDevice> deviceListener) {
		this(LocalSendProtocol.DEFAULT_PORT, deviceListener);
	}

	public LocalSendDiscovery(int port, Consumer<LocalSendDevice> deviceListener) {
		this.port = port;
		this.deviceListener = deviceListener;
	}

	public synchronized void start() {

		if (running) {
			return;
		}

		running = true;
		startMulticastListeners();
		startAnnouncementThread();

		Thread scanThread = new Thread(() -> {
			String localIp = getLocalIpAddress();
			if (localIp != null) {
				System.out.println("Starting LocalSend active subnet scan on: " + localIp);
				scanSubnet(localIp);
			}
		}, "LocalSend-SubnetScan");

		scanThread.setDaemon(true);
		scanThread.start();
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
			InetAddress group = InetAddress.getByName(LocalSendProtocol.MULTICAST_ADDRESS);

			while (interfaces.hasMoreElements()) {

				NetworkInterface networkInterface = interfaces.nextElement();

				if (!isUsableInterface(networkInterface)) {
					continue;
				}

				try {

					System.out.println("Starting LocalSend discovery on: " + networkInterface.getName() + " - "
							+ networkInterface.getDisplayName());

					Inet4Address ipv4 = Collections.list(networkInterface.getInetAddresses()).stream()
							.filter(Inet4Address.class::isInstance).map(Inet4Address.class::cast).findFirst()
							.orElse(null);

					if (ipv4 == null) {
						continue;
					}

					InetSocketAddress bindAddress = new InetSocketAddress(port);
					MulticastSocket socket = new MulticastSocket(null);
					socket.setReuseAddress(true);
					socket.setOption(StandardSocketOptions.SO_REUSEADDR, true);
					socket.bind(bindAddress);
					socket.joinGroup(new InetSocketAddress(group, port), networkInterface);
					synchronized (sockets) {
						sockets.add(socket);
					}

					System.out.println("Joined LocalSend multicast group " + group.getHostAddress() + ":" + port
							+ " on " + networkInterface.getName());

					Thread thread = new Thread(() -> listen(socket),
							"LocalSend-Multicast-" + networkInterface.getName());

					thread.setDaemon(true);
					thread.start();

				} catch (Exception e) {
					System.err.println("Unable to listen on " + networkInterface.getName() + ": " + e.getMessage());
				}
			}

		} catch (SocketException e) {
			System.err.println("Unable to enumerate network interfaces: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Unable to initialize LocalSend discovery: " + e.getMessage());
		}
	}

	private boolean isUsableInterface(NetworkInterface networkInterface) throws SocketException {
		if (!networkInterface.isUp() || networkInterface.isLoopback() || !networkInterface.supportsMulticast()) {
			return false;
		}

		return Collections.list(networkInterface.getInetAddresses()).stream()
				.anyMatch(addr -> addr instanceof Inet4Address || addr instanceof java.net.Inet6Address);
	}

	private void listen(MulticastSocket socket) {

		byte[] buffer = new byte[8192];

		while (running && !socket.isClosed()) {

			try {

				DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

				socket.receive(packet);

				String body = new String(packet.getData(), packet.getOffset(), packet.getLength(),
						StandardCharsets.UTF_8);

				System.out.println("LocalSend discovery packet received from " + packet.getAddress().getHostAddress()
						+ ":" + packet.getPort());

				System.out.println("LocalSend discovery data: " + body);

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
				System.out.println("Ignoring own LocalSend announcement");
				return;
			}

			boolean announce = json.optBoolean("announce", false);
			String address = packet.getAddress().getHostAddress();

			if (address.contains("%")) {
				address = address.substring(0, address.indexOf("%"));
			}

			System.out.println("Announce : " + announce + " | Address : " + address);

			// FIX: ALWAYS add the device regardless of whether announce is true or false
			addDevice(address, json);

			// If they are announcing, we must reply back directly
			if (announce) {
				sendMulticastResponse(packet.getAddress());
			}

		} catch (Exception e) {
			System.err.println("Invalid LocalSend discovery packet: " + e.getMessage());

			System.err.println("Packet data: "
					+ new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8));
		}
	}

	private void addDevice(String address, JSONObject json) {
		LocalSendDevice device = LocalSendDevice.fromJson(address, json);
		String myFingerprint = LocalSendIdentity.getFingerprint();

		if (device.getFingerprint().isBlank()) {
			System.err.println("Device payload missing fingerprint from " + address + ": " + json);
			return;
		}

		// Ignore self-discovery from loopback or IP scans
		if (device.getFingerprint().equals(myFingerprint)) {
			return;
		}

		boolean isNewDevice = !devices.containsKey(device.getFingerprint());
		devices.put(device.getFingerprint(), device);
		System.out.println("Valid external device added: " + device.getName() + " @ " + address);

		if (isNewDevice && deviceListener != null) {
			deviceListener.accept(device);
		}
	}

	private void sendMulticastResponse(InetAddress targetAddress) {
		try {
			JSONObject json = createDeviceJson();
			json.put("announce", false); // Response packet flag is false
			byte[] data = json.toString().getBytes(StandardCharsets.UTF_8);

			// Direct UDP packet back to target port 53317
			DatagramPacket packet = new DatagramPacket(data, data.length, targetAddress, LocalSendProtocol.DEFAULT_PORT);

			try (DatagramSocket socket = new DatagramSocket()) {
				socket.send(packet);
			}
		} catch (Exception e) {
			System.err.println("Failed to send discovery response: " + e.getMessage());
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

					System.out.println("LocalSend announcement sent to " + group.getHostAddress() + ":" + port
							+ " using " + socket.getLocalAddress());
				}
			}

		} catch (IOException e) {

			if (running) {
				System.err.println("Unable to send LocalSend announcement: " + e.getMessage());
			}
		}
	}

	public void scanSubnet(String localIp) {
		if (localIp == null || localIp.isBlank())
			return;

		String subnet = localIp.substring(0, localIp.lastIndexOf('.'));
		ExecutorService scanExecutor = Executors.newFixedThreadPool(30);

		for (int i = 1; i < 255; i++) {
			String targetIp = subnet + "." + i;

			if (targetIp.equals(localIp))
				continue;

			scanExecutor.submit(() -> checkDeviceAtIp(targetIp));
		}
		scanExecutor.shutdown();
	}

	private void checkDeviceAtIp(String ip) {
		try {
			URL url = new URL("https://" + ip + ":" + port + LocalSendProtocol.REGISTER_PATH);

			TrustManager[] trustAllCerts = new TrustManager[] {
				new X509TrustManager() {
					public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
					public void checkClientTrusted(X509Certificate[] certs, String authType) {}
					public void checkServerTrusted(X509Certificate[] certs, String authType) {}
				}
			};

			SSLContext sslContext = SSLContext.getInstance("TLS");
			sslContext.init(null, trustAllCerts, new SecureRandom());

			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			conn.setConnectTimeout(1500);
			conn.setReadTimeout(1500);
			conn.setSSLSocketFactory(sslContext.getSocketFactory());
			conn.setHostnameVerifier((hostname, session) -> true);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setDoOutput(true);

			JSONObject bodyJson = createDeviceJson();
			bodyJson.put("announce", false);
			byte[] payload = bodyJson.toString().getBytes(StandardCharsets.UTF_8);

			try (OutputStream os = conn.getOutputStream()) {
				os.write(payload);
			}

			if (conn.getResponseCode() == 200) {
				try (InputStream is = conn.getInputStream()) {
					String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
					JSONObject json = new JSONObject(body);
					addDevice(ip, json);
					System.out.println("LocalSend device discovered via direct IP scan: " + ip);
				}
			}
		} catch (java.net.SocketTimeoutException | java.net.ConnectException ignored) {
			// Normal for offline IPs in subnet
		} catch (Exception e) {
			System.err.println("Direct IP connection failed to " + ip + ": " + e.getMessage());
		}
	}

	private String getLocalIpAddress() {
		try {
			Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			while (interfaces.hasMoreElements()) {
				NetworkInterface ni = interfaces.nextElement();
				if (!isUsableInterface(ni))
					continue;

				Enumeration<InetAddress> addresses = ni.getInetAddresses();
				while (addresses.hasMoreElements()) {
					InetAddress addr = addresses.nextElement();
					if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
						return addr.getHostAddress();
					}
				}
			}
		} catch (Exception ignored) {
		}
		return null;
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