package com.oranbyte.screenrec.test;

import java.util.List;

import com.oranbyte.screenrec.gui.VideoPlayerPanel;
import com.oranbyte.screenrec.share.FileShareProvider;
import com.oranbyte.screenrec.share.ShareDevice;
import com.oranbyte.screenrec.share.localsend.LocalSendProvider;

public class MainApp {

	public static void main(String[] args) {

//		System.out.println("here1");
		FileShareProvider provider = new LocalSendProvider();
		
		System.out.println("here2");
		List<ShareDevice> devices = provider.getDevices();
		
		System.out.println("here3");
		for(ShareDevice device : devices) {
			System.out.println(device);
		}
		System.out.println("here4");
		
	
		 
		
	}
}
