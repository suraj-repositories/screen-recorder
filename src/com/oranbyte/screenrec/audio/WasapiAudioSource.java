package com.oranbyte.screenrec.audio;

import java.util.concurrent.locks.ReentrantLock;

import javax.sound.sampled.AudioFormat;

import com.sun.jna.Function;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.Ole32;
import com.sun.jna.platform.win32.WinNT.HANDLE;
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
	private static final int AUDCLNT_STREAMFLAGS_EVENTCALLBACK = 0x00040000;
	private static final long POLL_SLEEP_MS = 5L;

	private final Mode mode;
	private final ReentrantLock lifecycleLock = new ReentrantLock();

	private volatile boolean isPrepared = false;
	private volatile boolean isStarted = false;
	private Pointer pAudioClient = null;
	private Pointer pCaptureClient = null;
	private HANDLE hAudioEvent = null;

	// Event-driven mode is unsupported by many drivers when combined with
	// AUDCLNT_STREAMFLAGS_LOOPBACK (fails IAudioClient::Initialize with
	// AUDCLNT_E_UNSUPPORTED_FORMAT / 0x88890008). Loopback sources therefore
	// fall back to polling; plain capture sources keep using the event for
	// lower latency/CPU usage.
	private boolean eventDriven;

	private byte[] pendingBuffer = new byte[0];
	private int pendingOffset = 0;
	private int bytesPerFrame = 4;

	public WasapiAudioSource(Mode mode) {
		this.mode = mode;
	}

	/**
	 * Performs all the slow WASAPI setup (device enumeration, activation,
	 * format negotiation, event/service creation) WITHOUT starting the audio
	 * engine clock. Call {@link #engineStart()} on all prepared sources back
	 * to back to minimize the timing offset between independent devices
	 * (e.g. loopback vs. microphone) so their sample clocks start together.
	 */
	public void prepare(int sampleRate, int channels) throws Exception {
		lifecycleLock.lock();
		try {
			if (isPrepared) {
				return;
			}

			HRESULT hr = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED);
			if (hr.intValue() < 0 && hr.intValue() != 0x80010106) {
				checkHr(hr, "CoInitializeEx");
			}

			Pointer pEnumerator = createDeviceEnumerator();
			Pointer pDevice = getDevice(pEnumerator, mode == Mode.LOOPBACK ? 0 : 1);
			releaseComObject(pEnumerator);

			pAudioClient = activateAudioClient(pDevice);
			releaseComObject(pDevice);

			// Loopback + event-callback is rejected by many drivers (e.g. Realtek)
			// with AUDCLNT_E_UNSUPPORTED_FORMAT. Only use the event for non-loopback
			// (regular capture) sources.
			this.eventDriven = (mode != Mode.LOOPBACK);

			PointerByReference ppFormat = new PointerByReference();
			hr = WasapiNative.IAudioClient_GetMixFormat(pAudioClient, ppFormat);
			checkHr(hr, "IAudioClient::GetMixFormat");
			Pointer pWfex = ppFormat.getValue();

			try {
				short nChannels = pWfex.getShort(2);
				short wBitsPerSample = pWfex.getShort(14);
				short nBlockAlign = pWfex.getShort(12);
				this.bytesPerFrame = nBlockAlign > 0 ? nBlockAlign : (nChannels * (wBitsPerSample / 8));

				int flags = eventDriven ? AUDCLNT_STREAMFLAGS_EVENTCALLBACK : 0;
				if (mode == Mode.LOOPBACK) {
					flags |= AUDCLNT_STREAMFLAGS_LOOPBACK;
				}

				// 100ms buffer = 1,000,000 hns (hundred-nanosecond units)
				long hnsBufferDuration = 1000000L;
				long hnsPeriodicity = 0L; // Must be 0 in AUDCLNT_SHAREMODE_SHARED

				hr = WasapiNative.IAudioClient_Initialize(pAudioClient, AUDCLNT_SHAREMODE_SHARED, flags,
						hnsBufferDuration, hnsPeriodicity, pWfex, null);
				checkHr(hr, "IAudioClient::Initialize");
			} finally {
				Ole32.INSTANCE.CoTaskMemFree(pWfex);
			}

			if (eventDriven) {
				hAudioEvent = Kernel32.INSTANCE.CreateEvent(null, false, false, null);
				if (hAudioEvent == null) {
					throw new RuntimeException("Failed to create Windows Event handle.");
				}

				hr = WasapiNative.IAudioClient_SetEventHandle(pAudioClient, hAudioEvent);
				checkHr(hr, "IAudioClient::SetEventHandle");
			}

			PointerByReference ppCapture = new PointerByReference();
			hr = WasapiNative.IAudioClient_GetService(pAudioClient, WasapiNative.IID_IAudioCaptureClient, ppCapture);
			checkHr(hr, "IAudioClient::GetService(IAudioCaptureClient)");
			pCaptureClient = ppCapture.getValue();

			isPrepared = true;
		} finally {
			lifecycleLock.unlock();
		}
	}

	/**
	 * Starts the audio engine clock (IAudioClient::Start) on an already
	 * {@link #prepare(int, int)}d source. This is intentionally a single,
	 * cheap native call so that calling it on multiple sources in quick
	 * succession keeps their sample clocks closely aligned.
	 */
	public void engineStart() throws Exception {
		lifecycleLock.lock();
		try {
			if (isStarted || !isPrepared) {
				return;
			}
			HRESULT hr = WasapiNative.IAudioClient_Start(pAudioClient);
			checkHr(hr, "IAudioClient::Start");
			isStarted = true;
		} finally {
			lifecycleLock.unlock();
		}
	}

	@Override
	public void start(int sampleRate, int channels) throws Exception {
		prepare(sampleRate, channels);
		engineStart();
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws Exception {
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
			lifecycleLock.lock();
			try {
				if (!isStarted || pCaptureClient == null) {
					break;
				}

				IntByReference pFrames = new IntByReference();
				IntByReference pFlags = new IntByReference();
				PointerByReference ppData = new PointerByReference();

				HRESULT hr = WasapiNative.IAudioCaptureClient_GetBuffer(pCaptureClient, ppData, pFrames, pFlags, null,
						null);

				if (hr.intValue() == WasapiNative.AUDCLNT_S_BUFFER_EMPTY) {
					boolean useEvent = eventDriven && hAudioEvent != null;
					lifecycleLock.unlock();
					try {
						if (useEvent) {
							Kernel32.INSTANCE.WaitForSingleObject(hAudioEvent, 500);
						} else {
							Thread.sleep(POLL_SLEEP_MS);
						}
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						return totalRead;
					} finally {
						lifecycleLock.lock();
					}
					continue;
				}
				checkHr(hr, "IAudioCaptureClient::GetBuffer");

				int frames = pFrames.getValue();
				int flags = pFlags.getValue();
				int bytesAvailable = frames * bytesPerFrame;

				if (bytesAvailable > 0) {
					Pointer pData = ppData.getValue();
					byte[] packetData = new byte[bytesAvailable];

					if ((flags & WasapiNative.AUDCLNT_BUFFERFLAGS_SILENT) != 0
							|| (flags & WasapiNative.AUDCLNT_BUFFERFLAGS_DATA_DISCONTINUITY) != 0) {
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
			} finally {
				lifecycleLock.unlock();
			}
		}

		return totalRead;
	}

	@Override
	public void stop() {
		lifecycleLock.lock();
		try {
			if (!isStarted && !isPrepared) {
				return;
			}

			isStarted = false;
			isPrepared = false;

			if (pAudioClient != null) {
				WasapiNative.IAudioClient_Stop(pAudioClient);
				releaseComObject(pAudioClient);
				pAudioClient = null;
			}

			if (pCaptureClient != null) {
				releaseComObject(pCaptureClient);
				pCaptureClient = null;
			}

			if (hAudioEvent != null) {
				Kernel32.INSTANCE.CloseHandle(hAudioEvent);
				hAudioEvent = null;
			}

			Ole32.INSTANCE.CoUninitialize();
			pendingBuffer = new byte[0];
			pendingOffset = 0;
		} finally {
			lifecycleLock.unlock();
		}
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

	private void checkHr(HRESULT hr, String msg) {
		if (hr.intValue() < 0) {
			throw new RuntimeException("WASAPI Error [" + msg + "]: HRESULT 0x" + Integer.toHexString(hr.intValue()));
		}
	}

	private static class WasapiNative {
		public static final int AUDCLNT_S_BUFFER_EMPTY = 0x00010001;
		public static final int AUDCLNT_BUFFERFLAGS_DATA_DISCONTINUITY = 0x1;
		public static final int AUDCLNT_BUFFERFLAGS_SILENT = 0x2;
		public static final int AUDCLNT_BUFFERFLAGS_TIMESTAMP_ERROR = 0x4;

		public static final com.sun.jna.platform.win32.Guid.GUID CLSID_MMDeviceEnumerator = new com.sun.jna.platform.win32.Guid.GUID(
				"{BCDE0395-E52F-467C-8E3D-C4579291692E}");
		public static final com.sun.jna.platform.win32.Guid.GUID IID_IMMDeviceEnumerator = new com.sun.jna.platform.win32.Guid.GUID(
				"{A95664D2-9614-4F35-A746-DE8DB63617E6}");
		public static final com.sun.jna.platform.win32.Guid.GUID IID_IAudioClient = new com.sun.jna.platform.win32.Guid.GUID(
				"{1CB9AD4C-DBFA-4c32-B178-C2F568A703B2}");
		public static final com.sun.jna.platform.win32.Guid.GUID IID_IAudioCaptureClient = new com.sun.jna.platform.win32.Guid.GUID(
				"{C8ADBD64-E71E-48a0-A4DE-185C395CD317}");

		private static Function getVTableFunction(Pointer pThis, int vTableIndex) {
			Pointer vtbl = pThis.getPointer(0);
			Pointer pFunc = vtbl.getPointer((long) vTableIndex * Native.POINTER_SIZE);
			return Function.getFunction(pFunc, Function.ALT_CONVENTION);
		}

		public static int IUnknown_Release(Pointer pThis) {
			return getVTableFunction(pThis, 2).invokeInt(new Object[] { pThis });
		}

		public static HRESULT IMMDeviceEnumerator_GetDefaultAudioEndpoint(Pointer pThis, int dataFlow, int role,
				PointerByReference ppEndpoint) {
			int res = getVTableFunction(pThis, 4).invokeInt(new Object[] { pThis, dataFlow, role, ppEndpoint });
			return new HRESULT(res);
		}

		public static HRESULT IMMDevice_Activate(Pointer pThis, com.sun.jna.platform.win32.Guid.GUID iid, int dwClsCtx,
				Pointer pActivationParams, PointerByReference ppInterface) {
			int res = getVTableFunction(pThis, 3)
					.invokeInt(new Object[] { pThis, iid.getPointer(), dwClsCtx, pActivationParams, ppInterface });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_GetMixFormat(Pointer pThis, PointerByReference ppDeviceFormat) {
			int res = getVTableFunction(pThis, 8).invokeInt(new Object[] { pThis, ppDeviceFormat });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_Initialize(Pointer pThis, int ShareMode, int StreamFlags,
				long hnsBufferDuration, long hnsPeriodicity, Pointer pFormat, Pointer AudioSessionGuid) {
			int res = getVTableFunction(pThis, 3).invokeInt(new Object[] { pThis, ShareMode, StreamFlags,
					hnsBufferDuration, hnsPeriodicity, pFormat, AudioSessionGuid });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_SetEventHandle(Pointer pThis, HANDLE eventHandle) {
			int res = getVTableFunction(pThis, 13).invokeInt(new Object[] { pThis, eventHandle });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_Start(Pointer pThis) {
			int res = getVTableFunction(pThis, 10).invokeInt(new Object[] { pThis });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_Stop(Pointer pThis) {
			int res = getVTableFunction(pThis, 11).invokeInt(new Object[] { pThis });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_GetService(Pointer pThis, com.sun.jna.platform.win32.Guid.GUID riid,
				PointerByReference ppv) {
			int res = getVTableFunction(pThis, 14).invokeInt(new Object[] { pThis, riid.getPointer(), ppv });
			return new HRESULT(res);
		}

		public static HRESULT IAudioCaptureClient_GetBuffer(Pointer pThis, PointerByReference ppData,
				IntByReference pNumFramesToRead, IntByReference pdwFlags, Pointer pDevicePosition,
				Pointer pQPCPosition) {
			int res = getVTableFunction(pThis, 3).invokeInt(
					new Object[] { pThis, ppData, pNumFramesToRead, pdwFlags, pDevicePosition, pQPCPosition });
			return new HRESULT(res);
		}

		public static HRESULT IAudioCaptureClient_ReleaseBuffer(Pointer pThis, int NumFramesRead) {
			int res = getVTableFunction(pThis, 4).invokeInt(new Object[] { pThis, NumFramesRead });
			return new HRESULT(res);
		}
	}

	@Override
	public AudioFormat getCaptureFormat() {
		boolean tempInit = false;
		Pointer pAudioClientToRelease = null;
		Pointer pWfex = null;

		try {
			Pointer client = this.pAudioClient;
			if (client == null) {
				HRESULT hr = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED);
				if (hr.intValue() >= 0) {
					tempInit = true;
				}

				Pointer pEnumerator = createDeviceEnumerator();
				Pointer pDevice = getDevice(pEnumerator, mode == Mode.LOOPBACK ? 0 : 1);
				releaseComObject(pEnumerator);

				client = activateAudioClient(pDevice);
				releaseComObject(pDevice);
				pAudioClientToRelease = client;
			}

			PointerByReference ppFormat = new PointerByReference();
			HRESULT hr = WasapiNative.IAudioClient_GetMixFormat(client, ppFormat);
			checkHr(hr, "IAudioClient::GetMixFormat");
			pWfex = ppFormat.getValue();

			short wFormatTag = pWfex.getShort(0);
			short nChannels = pWfex.getShort(2);
			int nSamplesPerSec = pWfex.getInt(4);
			short wBitsPerSample = pWfex.getShort(14);
			short cbSize = pWfex.getShort(16);

			if ((wFormatTag & 0xFFFF) == 0xFFFE && cbSize >= 22) {
				short validBits = pWfex.getShort(18);
				if (validBits > 0) {
					wBitsPerSample = validBits;
				}

				int subFormatType = pWfex.getInt(24);
				if (subFormatType == 3) {
					wFormatTag = 3;
				} else if (subFormatType == 1) {
					wFormatTag = 1;
				}
			}

			AudioFormat.Encoding encoding = (wFormatTag == 3) ? AudioFormat.Encoding.PCM_FLOAT
					: AudioFormat.Encoding.PCM_SIGNED;

			int frameSize = (nChannels * wBitsPerSample) / 8;
			boolean bigEndian = false;

			return new AudioFormat(encoding, (float) nSamplesPerSec, wBitsPerSample, nChannels, frameSize,
					(float) nSamplesPerSec, bigEndian);

		} catch (Exception e) {
			return new AudioFormat(44100.0f, 16, 2, true, false);
		} finally {
			if (pWfex != null) {
				Ole32.INSTANCE.CoTaskMemFree(pWfex);
			}
			if (pAudioClientToRelease != null) {
				releaseComObject(pAudioClientToRelease);
			}
			if (tempInit) {
				Ole32.INSTANCE.CoUninitialize();
			}
		}
	}
}