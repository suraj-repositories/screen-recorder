package com.oranbyte.screenrec.share;

import java.io.File;
import java.util.List;

public interface FileShareProvider {

	void start();

	void stop();

	List<ShareDevice> getDevices();

	void send(File file, ShareDevice device, TransferListener listener);

	void cancel();
}