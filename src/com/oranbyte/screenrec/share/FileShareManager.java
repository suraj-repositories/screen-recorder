package com.oranbyte.screenrec.share;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import com.oranbyte.screenrec.share.localsend.LocalSendProvider;

public class FileShareManager {
 
	private static FileShareManager instance;

    public static synchronized FileShareManager getInstance() {
        if (instance == null) {
            instance = new FileShareManager(new LocalSendProvider());
        }
        return instance;
    }

    private final FileShareProvider provider;
    private volatile boolean started;

    public FileShareManager(FileShareProvider provider) {
        this.provider = Objects.requireNonNull(provider, "provider");
    }

	public synchronized void start() {

		if (started) {
			return;
		}

		provider.start();

		started = true;
	}

	public synchronized void stop() {

		if (!started) {
			return;
		}

		provider.stop();

		started = false;
	}

	public List<ShareDevice> getDevices() {
 
		if (!started) {
			return Collections.emptyList();
		}

		List<ShareDevice> devices = provider.getDevices();

		if (devices == null || devices.isEmpty()) {
			return Collections.emptyList();
		}

		return List.copyOf(devices);
	}

 
	public void send(File file, ShareDevice device, TransferListener listener) {

		Objects.requireNonNull(file, "file"); 
		Objects.requireNonNull(device, "device"); 
		Objects.requireNonNull(listener, "listener");

		if (!started) {
			throw new IllegalStateException("File sharing service is not started.");
		}

		if (!file.exists()) {
			throw new IllegalArgumentException("File does not exist: " + file.getAbsolutePath());
		}

		if (!file.isFile()) {
			throw new IllegalArgumentException("Path is not a file: " + file.getAbsolutePath());
		}

		if (!file.canRead()) {
			throw new IllegalArgumentException("File cannot be read: " + file.getAbsolutePath());
		}

		provider.send(file, device, listener);
	}

	public void cancel() {

		if (!started) {
			return;
		}

		provider.cancel();
	}

	public boolean isStarted() {
		return started;
	}
	
	public void startDiscovery() {
	    if (provider instanceof LocalSendProvider localSendProvider) {
	        localSendProvider.startDiscovery();
	    }
	}

	public void stopDiscovery() {
	    if (provider instanceof LocalSendProvider localSendProvider) {
	        localSendProvider.stopDiscovery();
	    }
	}
}