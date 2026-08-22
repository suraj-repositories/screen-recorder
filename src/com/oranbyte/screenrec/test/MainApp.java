package com.oranbyte.screenrec.test;

import java.util.List;

import com.oranbyte.screenrec.gui.VideoPlayerPanel;
import com.oranbyte.screenrec.share.FileShareProvider;
import com.oranbyte.screenrec.share.ShareDevice;
import com.oranbyte.screenrec.share.localsend.LocalSendDevice;
import com.oranbyte.screenrec.share.localsend.LocalSendDiscovery;
import com.oranbyte.screenrec.share.localsend.LocalSendProvider;

public class MainApp {

	public static void main(String[] args) {

//		System.out.println("here1");
//		FileShareProvider provider = new LocalSendProvider(); 
//		List<ShareDevice> devices = provider.getDevices(); 
//		for(ShareDevice device : devices) {
//			System.out.println(device);
//		} 
		
		LocalSendDiscovery discovery = new LocalSendDiscovery();

		discovery.start();
		List<LocalSendDevice> devices = discovery.getDevices();

		for (LocalSendDevice device : devices) {
		    System.out.println(
		        device.getName() + " - " +
		        device.getAddress() + ":" +
		        device.getPort()
		    );
		}
		 
		
	}
}
