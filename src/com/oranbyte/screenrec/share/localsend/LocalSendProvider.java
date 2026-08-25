package com.oranbyte.screenrec.share.localsend;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.oranbyte.screenrec.share.FileShareProvider;
import com.oranbyte.screenrec.share.ShareDevice;
import com.oranbyte.screenrec.share.TransferListener;

public class LocalSendProvider implements FileShareProvider {

	private final LocalSendDiscovery discovery;
	private final LocalSendServer server;
	private final LocalSendClient client;
	private final ExecutorService executor;
	private volatile boolean running;

	public LocalSendProvider() {
		discovery = new LocalSendDiscovery(LocalSendProtocol.DEFAULT_PORT);
		server = new LocalSendServer(LocalSendProtocol.DEFAULT_PORT, this::deviceDiscovered);
		client = new LocalSendClient();

		executor = Executors.newCachedThreadPool(runnable -> {
			Thread thread = new Thread(runnable, "LocalSend-Worker");
			thread.setDaemon(true);
			return thread;
		});
	}

	@Override
	public synchronized void start() {

		if (running) {
			return;
		}

		try {

			server.start();
			discovery.start();

			running = true;

			System.out.println("LocalSend provider started.");

		} catch (Exception e) {

			try {
				server.stop();
			} catch (Exception ignored) {
			}

			throw new IllegalStateException("Unable to start LocalSend.", e);
		}
	}

	@Override
	public synchronized void stop() {

		if (!running) {
			return;
		}

		discovery.stop();
		server.stop();
		executor.shutdownNow();
		running = false;

		System.out.println("LocalSend provider stopped.");
	}

	@Override
	public List<ShareDevice> getDevices() {

		if (!running) {
			return List.of();
		}

		return new ArrayList<>(discovery.getDevices());
	}

	@Override
	public void send(File file, ShareDevice device, TransferListener listener) {

		if (!running) {
			throw new IllegalStateException("LocalSend provider is not running.");
		}

		if (file == null) {
			throw new IllegalArgumentException("File cannot be null.");
		}

		if (!(device instanceof LocalSendDevice localDevice)) {
			throw new IllegalArgumentException("Device is not a LocalSend device.");
		}

		executor.submit(() -> {
			try {
				LocalSendFile localFile = new LocalSendFile(file);
				client.send(localDevice, localFile, listener);
			} catch (Exception e) {
				System.out.println("Error : " + e.getMessage());
				listener.onFailed(e);
			}
		});
	}

	@Override
	public void cancel() {
		client.cancelCurrentTransfer();
	}

	private void deviceDiscovered(LocalSendDevice device) {

		System.out.println("DEVICE FOUND: " + device.getName() + " @ " + device.getAddress());

	}

	public boolean isRunning() {
		return running;
	}

	public LocalSendDiscovery getDiscovery() {
		return discovery;
	}

	public LocalSendServer getServer() {
		return server;
	}
}