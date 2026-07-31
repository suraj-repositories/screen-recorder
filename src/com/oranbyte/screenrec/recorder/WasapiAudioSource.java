package com.oranbyte.screenrec.recorder;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.WinNT.HRESULT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;

public class WasapiAudioSource implements SystemAudioSource {

	public enum Mode {
		LOOPBACK, CAPTURE
	}

	private static final int CLSCTX_ALL = 23;
	private static final int AUDCLNT_SHAREMODE_SHARED = 0;
	private static final int AUDCLNT_STREAMFLAGS_LOOPBACK = 0x00020000;

	private final Mode mode;
	private Pointer pAudioClient = null;
	private Pointer pCaptureClient = null;
	private boolean isStarted = false;
	private byte[] pendingBuffer = new byte[0];
	private int pendingOffset = 0;
	private int bytesPerFrame = 4;

	public WasapiAudioSource(Mode mode) {
		this.mode = mode;
	}

	@Override
	public synchronized void start(int sampleRate, int channels) throws Exception {
		if (isStarted) {
			return;
		}

		HRESULT hr = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED);
		checkHr(hr, "CoInitializeEx");

		Pointer pEnumerator = createDeviceEnumerator();
		Pointer pDevice = getDevice(pEnumerator, mode == Mode.LOOPBACK ? 0 : 1);
		releaseComObject(pEnumerator);

		pAudioClient = activateAudioClient(pDevice);
		releaseComObject(pDevice);

		PointerByReference ppFormat = new PointerByReference();
		hr = WasapiNative.IAudioClient_GetMixFormat(pAudioClient, ppFormat);
		checkHr(hr, "IAudioClient::GetMixFormat");
		Pointer pWfex = ppFormat.getValue();

		short nChannels = pWfex.getShort(2);
		short wBitsPerSample = pWfex.getShort(14);
		short nBlockAlign = pWfex.getShort(12);
		this.bytesPerFrame = nBlockAlign > 0 ? nBlockAlign : (nChannels * (wBitsPerSample / 8));

		int flags = (mode == Mode.LOOPBACK) ? AUDCLNT_STREAMFLAGS_LOOPBACK : 0;

		hr = WasapiNative.IAudioClient_Initialize(pAudioClient, AUDCLNT_SHAREMODE_SHARED, flags, 10000000L, 0, pWfex,
				null);
		checkHr(hr, "IAudioClient::Initialize");

		PointerByReference ppCapture = new PointerByReference();
		hr = WasapiNative.IAudioClient_GetService(pAudioClient, WasapiNative.IID_IAudioCaptureClient, ppCapture);
		checkHr(hr, "IAudioClient::GetService(IAudioCaptureClient)");
		pCaptureClient = ppCapture.getValue();

		hr = WasapiNative.IAudioClient_Start(pAudioClient);
		checkHr(hr, "IAudioClient::Start");

		isStarted = true;
	}

	@Override
	public synchronized int read(byte[] buffer, int offset, int length) throws Exception {
		if (!isStarted) {
			return -1;
		}

		int totalRead = 0;

		if (pendingBuffer.length > 0) {
			int available = pendingBuffer.length - pendingOffset;
			int toCopy = Math.min(available, length);
			System.arraycopy(pendingBuffer, pendingOffset, buffer, offset, toCopy);
			pendingOffset += toCopy;
			totalRead += toCopy;

			if (pendingOffset >= pendingBuffer.length) {
				pendingBuffer = new byte[0];
				pendingOffset = 0;
			}

			if (totalRead == length) {
				return totalRead;
			}
		}

		while (totalRead < length && isStarted) {
			IntByReference pFrames = new IntByReference();
			IntByReference pFlags = new IntByReference();
			PointerByReference ppData = new PointerByReference();

			HRESULT hr = WasapiNative.IAudioCaptureClient_GetBuffer(pCaptureClient, ppData, pFrames, pFlags, null,
					null);

			if (hr.intValue() == WasapiNative.AUDCLNT_S_BUFFER_EMPTY) {
				Thread.sleep(2);
				continue;
			}
			checkHr(hr, "IAudioCaptureClient::GetBuffer");

			int frames = pFrames.getValue();
			int bytesAvailable = frames * bytesPerFrame;

			if (bytesAvailable > 0) {
				Pointer pData = ppData.getValue();
				byte[] packetData = new byte[bytesAvailable];

				if ((pFlags.getValue() & WasapiNative.AUDCLNT_BUFFERFLAGS_SILENT) != 0) {
					java.util.Arrays.fill(packetData, (byte) 0);
				} else if (pData != null) {
					pData.read(0, packetData, 0, bytesAvailable);
				}

				int needed = length - totalRead;
				int toCopy = Math.min(needed, bytesAvailable);
				System.arraycopy(packetData, 0, buffer, offset + totalRead, toCopy);
				totalRead += toCopy;

				if (toCopy < bytesAvailable) {
					int leftover = bytesAvailable - toCopy;
					pendingBuffer = new byte[leftover];
					System.arraycopy(packetData, toCopy, pendingBuffer, 0, leftover);
					pendingOffset = 0;
				}
			}

			WasapiNative.IAudioCaptureClient_ReleaseBuffer(pCaptureClient, frames);
		}

		return totalRead;
	}

	@Override
	public synchronized void stop() {
		if (!isStarted) {
			return;
		}

		if (pAudioClient != null) {
			WasapiNative.IAudioClient_Stop(pAudioClient);
			releaseComObject(pAudioClient);
			pAudioClient = null;
		}

		if (pCaptureClient != null) {
			releaseComObject(pCaptureClient);
			pCaptureClient = null;
		}

		Ole32.INSTANCE.CoUninitialize();
		isStarted = false;
		pendingBuffer = new byte[0];
		pendingOffset = 0;
	}

	private Pointer createDeviceEnumerator() throws Exception {
		PointerByReference ppEnum = new PointerByReference();
		HRESULT hr = Ole32.INSTANCE.CoCreateInstance(WasapiNative.CLSID_MMDeviceEnumerator, null, CLSCTX_ALL,
				WasapiNative.IID_IMMDeviceEnumerator, ppEnum);
		checkHr(hr, "CoCreateInstance(MMDeviceEnumerator)");
		return ppEnum.getValue();
	}

	private Pointer getDevice(Pointer pEnumerator, int dataFlow) throws Exception {
		PointerByReference ppDevice = new PointerByReference();
		HRESULT hr = WasapiNative.IMMDeviceEnumerator_GetDefaultAudioEndpoint(pEnumerator, dataFlow, 0, ppDevice);
		checkHr(hr, "IMMDeviceEnumerator::GetDefaultAudioEndpoint");
		return ppDevice.getValue();
	}

	private Pointer activateAudioClient(Pointer pDevice) throws Exception {
		PointerByReference ppClient = new PointerByReference();
		HRESULT hr = WasapiNative.IMMDevice_Activate(pDevice, WasapiNative.IID_IAudioClient, CLSCTX_ALL, null,
				ppClient);
		checkHr(hr, "IMMDevice::Activate");
		return ppClient.getValue();
	}

	private void releaseComObject(Pointer pUnk) {
		if (pUnk != null) {
			WasapiNative.IUnknown_Release(pUnk);
		}
	}

	private void checkHr(HRESULT hr, String msg) throws Exception {
		if (hr.intValue() < 0) {
			throw new RuntimeException("WASAPI Error [" + msg + "]: HRESULT 0x" + Integer.toHexString(hr.intValue()));
		}
	}

	private static class WasapiNative {
		public static final int AUDCLNT_S_BUFFER_EMPTY = 0x00010001;
		public static final int AUDCLNT_BUFFERFLAGS_SILENT = 0x2;

		public static final com.sun.jna.platform.win32.Guid.GUID CLSID_MMDeviceEnumerator = new com.sun.jna.platform.win32.Guid.GUID(
				"{BCDE0395-E52F-467C-8E3D-C4579291692E}");
		public static final com.sun.jna.platform.win32.Guid.GUID IID_IMMDeviceEnumerator = new com.sun.jna.platform.win32.Guid.GUID(
				"{A95664D2-9614-4F35-A746-DE8DB63617E6}");
		public static final com.sun.jna.platform.win32.Guid.GUID IID_IAudioClient = new com.sun.jna.platform.win32.Guid.GUID(
				"{1CB9AD4C-DBFA-4c32-B178-C2F568A703B2}");
		public static final com.sun.jna.platform.win32.Guid.GUID IID_IAudioCaptureClient = new com.sun.jna.platform.win32.Guid.GUID(
				"{C8ADBD64-E71E-48a0-A4DE-185C395CD317}");

		public static int IUnknown_Release(Pointer pThis) {
			Pointer vtbl = pThis.getPointer(0);
			Pointer pFunc = vtbl.getPointer(2 * Native.POINTER_SIZE);
			com.sun.jna.Function func = com.sun.jna.Function.getFunction(pFunc, com.sun.jna.Function.ALT_CONVENTION);
			return func.invokeInt(new Object[] { pThis });
		}

		public static HRESULT IMMDeviceEnumerator_GetDefaultAudioEndpoint(Pointer pThis, int dataFlow, int role,
				PointerByReference ppEndpoint) {
			Pointer vtbl = pThis.getPointer(0);
			Pointer pFunc = vtbl.getPointer(4 * Native.POINTER_SIZE);
			com.sun.jna.Function func = com.sun.jna.Function.getFunction(pFunc, com.sun.jna.Function.ALT_CONVENTION);
			int res = func.invokeInt(new Object[] { pThis, dataFlow, role, ppEndpoint });
			return new HRESULT(res);
		}

		public static HRESULT IMMDevice_Activate(Pointer pThis, com.sun.jna.platform.win32.Guid.GUID iid, int dwClsCtx,
				Pointer pActivationParams, PointerByReference ppInterface) {
			Pointer vtbl = pThis.getPointer(0);
			Pointer pFunc = vtbl.getPointer(3 * Native.POINTER_SIZE);
			com.sun.jna.Function func = com.sun.jna.Function.getFunction(pFunc, com.sun.jna.Function.ALT_CONVENTION);
			int res = func.invokeInt(new Object[] { pThis, iid, dwClsCtx, pActivationParams, ppInterface });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_GetMixFormat(Pointer pThis, PointerByReference ppDeviceFormat) {
			Pointer vtbl = pThis.getPointer(0);
			Pointer pFunc = vtbl.getPointer(8 * Native.POINTER_SIZE);
			com.sun.jna.Function func = com.sun.jna.Function.getFunction(pFunc, com.sun.jna.Function.ALT_CONVENTION);
			int res = func.invokeInt(new Object[] { pThis, ppDeviceFormat });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_Initialize(Pointer pThis, int ShareMode, int StreamFlags,
				long hnsBufferDuration, long hnsPeriodicity, Pointer pFormat, Pointer AudioSessionGuid) {
			Pointer vtbl = pThis.getPointer(0);
			Pointer pFunc = vtbl.getPointer(3 * Native.POINTER_SIZE);
			com.sun.jna.Function func = com.sun.jna.Function.getFunction(pFunc, com.sun.jna.Function.ALT_CONVENTION);
			int res = func.invokeInt(new Object[] { pThis, ShareMode, StreamFlags, hnsBufferDuration, hnsPeriodicity,
					pFormat, AudioSessionGuid });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_Start(Pointer pThis) {
			Pointer vtbl = pThis.getPointer(0);
			Pointer pFunc = vtbl.getPointer(10 * Native.POINTER_SIZE);
			com.sun.jna.Function func = com.sun.jna.Function.getFunction(pFunc, com.sun.jna.Function.ALT_CONVENTION);
			int res = func.invokeInt(new Object[] { pThis });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_Stop(Pointer pThis) {
			Pointer vtbl = pThis.getPointer(0);
			Pointer pFunc = vtbl.getPointer(11 * Native.POINTER_SIZE);
			com.sun.jna.Function func = com.sun.jna.Function.getFunction(pFunc, com.sun.jna.Function.ALT_CONVENTION);
			int res = func.invokeInt(new Object[] { pThis });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_GetService(Pointer pThis, com.sun.jna.platform.win32.Guid.GUID riid,
				PointerByReference ppv) {
			Pointer vtbl = pThis.getPointer(0);
			Pointer pFunc = vtbl.getPointer(14 * Native.POINTER_SIZE);
			com.sun.jna.Function func = com.sun.jna.Function.getFunction(pFunc, com.sun.jna.Function.ALT_CONVENTION);
			int res = func.invokeInt(new Object[] { pThis, riid, ppv });
			return new HRESULT(res);
		}

		public static HRESULT IAudioCaptureClient_GetBuffer(Pointer pThis, PointerByReference ppData,
				IntByReference pNumFramesToRead, IntByReference pdwFlags, Pointer pDevicePosition,
				Pointer pQPCPosition) {
			Pointer vtbl = pThis.getPointer(0);
			Pointer pFunc = vtbl.getPointer(3 * Native.POINTER_SIZE);
			com.sun.jna.Function func = com.sun.jna.Function.getFunction(pFunc, com.sun.jna.Function.ALT_CONVENTION);
			int res = func.invokeInt(
					new Object[] { pThis, ppData, pNumFramesToRead, pdwFlags, pDevicePosition, pQPCPosition });
			return new HRESULT(res);
		}

		public static HRESULT IAudioCaptureClient_ReleaseBuffer(Pointer pThis, int NumFramesRead) {
			Pointer vtbl = pThis.getPointer(0);
			Pointer pFunc = vtbl.getPointer(4 * Native.POINTER_SIZE);
			com.sun.jna.Function func = com.sun.jna.Function.getFunction(pFunc, com.sun.jna.Function.ALT_CONVENTION);
			int res = func.invokeInt(new Object[] { pThis, NumFramesRead });
			return new HRESULT(res);
		}
	}
}