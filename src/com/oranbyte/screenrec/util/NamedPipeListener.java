package com.oranbyte.screenrec.util;

import java.nio.charset.StandardCharsets;

import com.oranbyte.screenrec.util.NotificationUtil.NotificationClickListener;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.ptr.IntByReference;

public class NamedPipeListener {

	private final String pipeName;
	private final NotificationClickListener listener;

	public NamedPipeListener(String pipeName, NotificationClickListener listener) {
		this.pipeName = pipeName;
		this.listener = listener;
	}

	public void start() {

		Thread t = new Thread(this::listen, "Toast-Pipe");

		t.setDaemon(true);
		t.start();
	}

	private void listen() {

		String fullPipe = "\\\\.\\pipe\\" + pipeName;

		while (true) {

			HANDLE pipe = Kernel32.INSTANCE.CreateNamedPipe(fullPipe, WinBase.PIPE_ACCESS_DUPLEX,
					WinBase.PIPE_TYPE_MESSAGE | WinBase.PIPE_READMODE_MESSAGE | WinBase.PIPE_WAIT, 1, 4096, 4096, 0,
					null);

			if (WinBase.INVALID_HANDLE_VALUE.equals(pipe)) {
				continue;
			}

			boolean connected = Kernel32.INSTANCE.ConnectNamedPipe(pipe, null);

			if (!connected) {
				Kernel32.INSTANCE.CloseHandle(pipe);
				continue;
			}

			byte[] buffer = new byte[4096];

			IntByReference read = new IntByReference();

			boolean ok = Kernel32.INSTANCE.ReadFile(pipe, buffer, buffer.length, read, null);

			if (ok) {
				String message = new String(buffer, 0, read.getValue(), StandardCharsets.UTF_8);

				System.out.println("Pipe received: " + message);

				if (listener != null) {
					listener.onClick();
				}
			}

			Kernel32.INSTANCE.DisconnectNamedPipe(pipe);
			Kernel32.INSTANCE.CloseHandle(pipe);
		}
	}
}