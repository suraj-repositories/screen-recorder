package com.oranbyte.screenrec.test;

import java.util.List;

import com.oranbyte.screenrec.gui.VideoPlayerPanel;
import com.oranbyte.screenrec.share.FileShareProvider;
import com.oranbyte.screenrec.share.ShareDevice;
import com.oranbyte.screenrec.share.localsend.LocalSendDevice;
import com.oranbyte.screenrec.share.localsend.LocalSendDiscovery;
import com.oranbyte.screenrec.share.localsend.LocalSendProtocol;
import com.oranbyte.screenrec.share.localsend.LocalSendProvider;
import com.oranbyte.screenrec.share.localsend.LocalSendServer;

public class MainApp {

	public static void main(String[] args) {

		LocalSendDiscovery discovery = new LocalSendDiscovery();

		LocalSendServer server = new LocalSendServer(LocalSendProtocol.DEFAULT_PORT, device -> {
			System.out.println(
					"DEVICE FOUND: " + device.getName() + " - " + device.getAddress() + ":" + device.getPort());
		});

		try {

			server.start();
			discovery.start();

			System.out.println("LocalSend discovery running...");

			while (true) {
				Thread.sleep(1000);
				List<LocalSendDevice> devices = discovery.getDevices();
				System.out.println("Discovered devices: " + devices.size());
			}

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			discovery.stop();
			server.stop();
		}

	}
}
