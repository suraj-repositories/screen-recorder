package com.oranbyte.screenrec.test;

import java.util.List;

import com.oranbyte.screenrec.share.FileShareProvider;
import com.oranbyte.screenrec.share.ShareDevice;
import com.oranbyte.screenrec.share.localsend.LocalSendProvider;

public class MainApp {

	public static void main(String[] args) {

		FileShareProvider provider = new LocalSendProvider();
		
		List<ShareDevice> devices = provider.getDevices();
		
		for(ShareDevice device : devices) {
			System.out.println(device);
		}
		
	}
}
