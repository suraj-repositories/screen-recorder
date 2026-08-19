package com.oranbyte.screenrec.audio;

import java.util.concurrent.locks.ReentrantLock;
import javax.sound.sampled.AudioFormat;

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
	private static final int AUDCLNT_STREAMFLAGS_AUTOCONVERTPCM = 0x80000000;
	private static final int AUDCLNT_E_UNSUPPORTED_FORMAT = 0x88890008;
	private static final long POLL_SLEEP_MS = 3L;

	private final Mode mode;
	private final ReentrantLock lifecycleLock = new ReentrantLock();

	private volatile boolean isPrepared = false;
	private volatile boolean isStarted = false;
	private Pointer pAudioClient = null;
	private Pointer pCaptureClient = null;
	private HANDLE hAudioEvent = null;
	private boolean eventDriven;

	private byte[] pendingBuffer = new byte[0];
	private int pendingOffset = 0;
	private int bytesPerFrame = 4;
	private AudioFormat cachedFormat = null;

	private WasapiSilenceGenerator silenceGenerator = null;

	public WasapiAudioSource(Mode mode) {
		this.mode = mode;
	}

	public void prepare(int targetSampleRate, int targetChannels) throws Exception {
		lifecycleLock.lock();
		try {
			if (isPrepared) {
				return;
			}

			HRESULT hr = Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED);
			if (hr.intValue() < 0 && hr.intValue() != 0x80010106) {
				checkHr(hr, "CoInitializeEx");
			}

			if (mode == Mode.LOOPBACK) {
				silenceGenerator = new WasapiSilenceGenerator();
				silenceGenerator.start();
			}

			Pointer pEnumerator = createDeviceEnumerator();
			Pointer pDevice = getDevice(pEnumerator, mode == Mode.LOOPBACK ? 0 : 1);
			releaseComObject(pEnumerator);

			pAudioClient = activateAudioClient(pDevice);
			releaseComObject(pDevice);

			this.eventDriven = true;

			PointerByReference ppFormat = new PointerByReference();
			hr = WasapiNative.IAudioClient_GetMixFormat(pAudioClient, ppFormat);
			checkHr(hr, "IAudioClient::GetMixFormat");
			Pointer pWfex = ppFormat.getValue();

			try {
				long hnsBufferDuration = 10000000L;
				long hnsPeriodicity = 0L;

				int baseFlags = (mode == Mode.LOOPBACK) ? AUDCLNT_STREAMFLAGS_LOOPBACK : 0;

				pWfex = initializeAudioClient(pAudioClient, pWfex, baseFlags, hnsBufferDuration, hnsPeriodicity);

				short nChannels = pWfex.getShort(2);
				short nBlockAlign = pWfex.getShort(12);
				short wBitsPerSample = pWfex.getShort(14);

				this.bytesPerFrame = nBlockAlign > 0 ? nBlockAlign : (nChannels * (wBitsPerSample / 8));
				this.cachedFormat = parseWaveFormatEx(pWfex);

			} finally {
				if (pWfex != null) {
					Ole32.INSTANCE.CoTaskMemFree(pWfex);
				}
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

	private Pointer initializeAudioClient(Pointer audioClient, Pointer mixFormat, int baseFlags, long bufferDuration,
			long periodicity) throws Exception {

		HRESULT hr;

		eventDriven = true;

		int flags = baseFlags | AUDCLNT_STREAMFLAGS_EVENTCALLBACK;

		hr = WasapiNative.IAudioClient_Initialize(audioClient, AUDCLNT_SHAREMODE_SHARED, flags, bufferDuration,
				periodicity, mixFormat, null);

		if (hr.intValue() >= 0) {
			return mixFormat;
		}

		eventDriven = false;

		flags = baseFlags;

		hr = WasapiNative.IAudioClient_Initialize(audioClient, AUDCLNT_SHAREMODE_SHARED, flags, bufferDuration,
				periodicity, mixFormat, null);

		if (hr.intValue() >= 0) {
			return mixFormat;
		}

		flags = baseFlags | AUDCLNT_STREAMFLAGS_AUTOCONVERTPCM;

		hr = WasapiNative.IAudioClient_Initialize(audioClient, AUDCLNT_SHAREMODE_SHARED, flags, bufferDuration,
				periodicity, mixFormat, null);

		if (hr.intValue() >= 0) {
			eventDriven = false;
			return mixFormat;
		}

		eventDriven = true;

		flags = baseFlags | AUDCLNT_STREAMFLAGS_EVENTCALLBACK | AUDCLNT_STREAMFLAGS_AUTOCONVERTPCM;

		hr = WasapiNative.IAudioClient_Initialize(audioClient, AUDCLNT_SHAREMODE_SHARED, flags, bufferDuration,
				periodicity, mixFormat, null);

		if (hr.intValue() >= 0) {
			return mixFormat;
		}

		PointerByReference ppClosestMatch = new PointerByReference();

		HRESULT supportHr = WasapiNative.IAudioClient_IsFormatSupported(audioClient, AUDCLNT_SHAREMODE_SHARED,
				mixFormat, ppClosestMatch);

		Pointer closestFormat = ppClosestMatch.getValue();

		if (supportHr.intValue() >= 0 && closestFormat != null) {

			try {
				eventDriven = false;

				flags = baseFlags;

				hr = WasapiNative.IAudioClient_Initialize(audioClient, AUDCLNT_SHAREMODE_SHARED, flags, bufferDuration,
						periodicity, closestFormat, null);

				if (hr.intValue() >= 0) {
					return closestFormat;
				}

				eventDriven = true;

				flags = baseFlags | AUDCLNT_STREAMFLAGS_EVENTCALLBACK;

				hr = WasapiNative.IAudioClient_Initialize(audioClient, AUDCLNT_SHAREMODE_SHARED, flags, bufferDuration,
						periodicity, closestFormat, null);

				if (hr.intValue() >= 0) {
					return closestFormat;
				}

			} catch (Exception e) {
				Ole32.INSTANCE.CoTaskMemFree(closestFormat);
				throw e;
			}

			Ole32.INSTANCE.CoTaskMemFree(closestFormat);
		}

		throw new RuntimeException("WASAPI could not initialize the audio device. " + "Last HRESULT: 0x"
				+ Integer.toHexString(hr.intValue()) + ", IsFormatSupported: 0x"
				+ Integer.toHexString(supportHr.intValue()));
	}

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
							Kernel32.INSTANCE.WaitForSingleObject(hAudioEvent, 20);
						} else {
							Thread.sleep(POLL_SLEEP_MS);
						}
					} catch (InterruptedException ie) {
						Thread.currentThread().interrupt();
						return totalRead;
					} finally {
						lifecycleLock.lock();
					}

					int neededBytes = length - totalRead;
					int frameSize = Math.max(bytesPerFrame, 4);
					int silentBytes = Math.min(neededBytes, frameSize * 128);

					byte[] silentBuffer = new byte[silentBytes];
					System.arraycopy(silentBuffer, 0, buffer, offset + totalRead, silentBytes);
					totalRead += silentBytes;

					continue;
				}
				checkHr(hr, "IAudioCaptureClient::GetBuffer");

				int frames = pFrames.getValue();
				int flags = pFlags.getValue();
				int bytesAvailable = frames * bytesPerFrame;

				if (bytesAvailable > 0) {
					Pointer pData = ppData.getValue();
					byte[] packetData = new byte[bytesAvailable];

					if ((flags & WasapiNative.AUDCLNT_BUFFERFLAGS_SILENT) != 0) {
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

			if (silenceGenerator != null) {
				silenceGenerator.stopGenerator();
				silenceGenerator = null;
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

			if (hAudioEvent != null) {
				Kernel32.INSTANCE.CloseHandle(hAudioEvent);
				hAudioEvent = null;
			}

			Ole32.INSTANCE.CoUninitialize();
			pendingBuffer = new byte[0];
			pendingOffset = 0;
			cachedFormat = null;
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
		int value = hr.intValue();

		if (value < 0) {
			throw new RuntimeException("WASAPI Error [" + msg + "]: HRESULT 0x" + String.format("%08X", value));
		}
	}

	@Override
	public AudioFormat getCaptureFormat() {
		if (cachedFormat != null) {
			return cachedFormat;
		}

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

			return parseWaveFormatEx(pWfex);

		} catch (Exception e) {
			return new AudioFormat(48000.0f, 16, 2, true, false);
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

	private AudioFormat parseWaveFormatEx(Pointer pWfex) {
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
		return new AudioFormat(encoding, (float) nSamplesPerSec, wBitsPerSample, nChannels, frameSize,
				(float) nSamplesPerSec, false);
	}

	private static class WasapiSilenceGenerator extends Thread {
		private volatile boolean running = true;

		public WasapiSilenceGenerator() {
			setDaemon(true);
			setName("WASAPI-Silence-Generator");
		}

		public void stopGenerator() {
			running = false;
			interrupt();
		}

		@Override
		public void run() {
			Pointer pAudioClient = null;
			Pointer pRenderClient = null;

			try {
				Ole32.INSTANCE.CoInitializeEx(null, Ole32.COINIT_MULTITHREADED);

				PointerByReference ppEnum = new PointerByReference();
				WasapiNative.CoCreateInstance(WasapiNative.CLSID_MMDeviceEnumerator, null, 23,
						WasapiNative.IID_IMMDeviceEnumerator, ppEnum);
				Pointer pEnumerator = ppEnum.getValue();

				PointerByReference ppDevice = new PointerByReference();
				HRESULT hr = WasapiNative.IMMDeviceEnumerator_GetDefaultAudioEndpoint(pEnumerator, 0, 0, ppDevice);
				if (hr.intValue() < 0) {
					WasapiNative.IUnknown_Release(pEnumerator);
					return;
				}
				Pointer pDevice = ppDevice.getValue();
				WasapiNative.IUnknown_Release(pEnumerator);

				PointerByReference ppClient = new PointerByReference();
				hr = WasapiNative.IMMDevice_Activate(pDevice, WasapiNative.IID_IAudioClient, 23, null, ppClient);
				if (hr.intValue() < 0) {
					WasapiNative.IUnknown_Release(pDevice);
					return;
				}
				pAudioClient = ppClient.getValue();
				WasapiNative.IUnknown_Release(pDevice);

				PointerByReference ppFormat = new PointerByReference();
				hr = WasapiNative.IAudioClient_GetMixFormat(pAudioClient, ppFormat);
				if (hr.intValue() < 0) {
					return;
				}
				Pointer pWfex = ppFormat.getValue();

				try {
					hr = WasapiNative.IAudioClient_Initialize(pAudioClient, 0, 0, 10000000L, 0L, pWfex, null);
					if (hr.intValue() < 0) {
						return;
					}
				} finally {
					Ole32.INSTANCE.CoTaskMemFree(pWfex);
				}

				PointerByReference ppRender = new PointerByReference();
				hr = WasapiNative.IAudioClient_GetService(pAudioClient, WasapiNative.IID_IAudioRenderClient, ppRender);
				if (hr.intValue() < 0) {
					return;
				}
				pRenderClient = ppRender.getValue();

				WasapiNative.IAudioClient_Start(pAudioClient);

				IntByReference pPadding = new IntByReference();

				while (running && !isInterrupted()) {
					hr = WasapiNative.IAudioClient_GetCurrentPadding(pAudioClient, pPadding);
					if (hr.intValue() >= 0) {
						int numFramesPadding = pPadding.getValue();
						int bufferFrameCount = 2000;
						int numFramesAvailable = bufferFrameCount - numFramesPadding;

						if (numFramesAvailable > 0) {
							PointerByReference ppData = new PointerByReference();
							hr = WasapiNative.IAudioRenderClient_GetBuffer(pRenderClient, numFramesAvailable, ppData);
							if (hr.intValue() >= 0 && ppData.getValue() != null) {
								WasapiNative.IAudioRenderClient_ReleaseBuffer(pRenderClient, numFramesAvailable, 0x2);
							}
						}
					}
					Thread.sleep(20);
				}
			} catch (Exception ignored) {
			} finally {
				if (pRenderClient != null)
					WasapiNative.IUnknown_Release(pRenderClient);
				if (pAudioClient != null) {
					WasapiNative.IAudioClient_Stop(pAudioClient);
					WasapiNative.IUnknown_Release(pAudioClient);
				}
				Ole32.INSTANCE.CoUninitialize();
			}
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
		public static final com.sun.jna.platform.win32.Guid.GUID IID_IAudioRenderClient = new com.sun.jna.platform.win32.Guid.GUID(
				"{F294BCFC-3139-470A-A059-071345D11558}");

		private static Pointer getVTablePointer(Pointer pThis, int vTableIndex) {
			Pointer vtbl = pThis.getPointer(0);
			return vtbl.getPointer((long) vTableIndex * Native.POINTER_SIZE);
		}

		public static HRESULT CoCreateInstance(com.sun.jna.platform.win32.Guid.GUID rclsid, Pointer pUnkOuter,
				int dwClsContext, com.sun.jna.platform.win32.Guid.GUID riid, PointerByReference ppv) {
			return Ole32.INSTANCE.CoCreateInstance(rclsid, pUnkOuter, dwClsContext, riid, ppv);
		}

		public static int IUnknown_Release(Pointer pThis) {
			com.sun.jna.Function f = com.sun.jna.Function.getFunction(getVTablePointer(pThis, 2),
					com.sun.jna.Function.ALT_CONVENTION);
			return f.invokeInt(new Object[] { pThis });
		}

		public static HRESULT IMMDeviceEnumerator_GetDefaultAudioEndpoint(Pointer pThis, int dataFlow, int role,
				PointerByReference ppEndpoint) {
			com.sun.jna.Function f = com.sun.jna.Function.getFunction(getVTablePointer(pThis, 4),
					com.sun.jna.Function.ALT_CONVENTION);
			int res = f.invokeInt(new Object[] { pThis, dataFlow, role, ppEndpoint });
			return new HRESULT(res);
		}

		public static HRESULT IMMDevice_Activate(Pointer pThis, com.sun.jna.platform.win32.Guid.GUID iid, int dwClsCtx,
				Pointer pActivationParams, PointerByReference ppInterface) {
			com.sun.jna.Function f = com.sun.jna.Function.getFunction(getVTablePointer(pThis, 3),
					com.sun.jna.Function.ALT_CONVENTION);
			int res = f.invokeInt(new Object[] { pThis, iid.getPointer(), dwClsCtx, pActivationParams, ppInterface });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_GetMixFormat(Pointer pThis, PointerByReference ppDeviceFormat) {
			com.sun.jna.Function f = com.sun.jna.Function.getFunction(getVTablePointer(pThis, 8),
					com.sun.jna.Function.ALT_CONVENTION);
			int res = f.invokeInt(new Object[] { pThis, ppDeviceFormat });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_IsFormatSupported(Pointer pThis, int ShareMode, Pointer pFormat,
				PointerByReference ppClosestMatch) {
			com.sun.jna.Function f = com.sun.jna.Function.getFunction(getVTablePointer(pThis, 9),
					com.sun.jna.Function.ALT_CONVENTION);
			int res = f.invokeInt(new Object[] { pThis, ShareMode, pFormat, ppClosestMatch });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_Initialize(Pointer pThis, int ShareMode, int StreamFlags,
				long hnsBufferDuration, long hnsPeriodicity, Pointer pFormat, Pointer AudioSessionGuid) {
			com.sun.jna.Function f = com.sun.jna.Function.getFunction(getVTablePointer(pThis, 3),
					com.sun.jna.Function.ALT_CONVENTION);
			int res = f.invokeInt(new Object[] { pThis, ShareMode, StreamFlags, hnsBufferDuration, hnsPeriodicity,
					pFormat, AudioSessionGuid });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_GetCurrentPadding(Pointer pThis, IntByReference pNumPaddingFrames) {
			com.sun.jna.Function f = com.sun.jna.Function.getFunction(getVTablePointer(pThis, 6),
					com.sun.jna.Function.ALT_CONVENTION);
			int res = f.invokeInt(new Object[] { pThis, pNumPaddingFrames });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_SetEventHandle(Pointer pThis, HANDLE eventHandle) {
			com.sun.jna.Function f = com.sun.jna.Function.getFunction(getVTablePointer(pThis, 13),
					com.sun.jna.Function.ALT_CONVENTION);
			int res = f.invokeInt(new Object[] { pThis, eventHandle });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_Start(Pointer pThis) {
			com.sun.jna.Function f = com.sun.jna.Function.getFunction(getVTablePointer(pThis, 10),
					com.sun.jna.Function.ALT_CONVENTION);
			int res = f.invokeInt(new Object[] { pThis });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_Stop(Pointer pThis) {
			com.sun.jna.Function f = com.sun.jna.Function.getFunction(getVTablePointer(pThis, 11),
					com.sun.jna.Function.ALT_CONVENTION);
			int res = f.invokeInt(new Object[] { pThis });
			return new HRESULT(res);
		}

		public static HRESULT IAudioClient_GetService(Pointer pThis, com.sun.jna.platform.win32.Guid.GUID riid,
				PointerByReference ppv) {
			com.sun.jna.Function f = com.sun.jna.Function.getFunction(getVTablePointer(pThis, 14),
					com.sun.jna.Function.ALT_CONVENTION);
			int res = f.invokeInt(new Object[] { pThis, riid.getPointer(), ppv });
			return new HRESULT(res);
		}

		public static HRESULT IAudioCaptureClient_GetBuffer(Pointer pThis, PointerByReference ppData,
				IntByReference pNumFramesToRead, IntByReference pdwFlags, Pointer pDevicePosition,
				Pointer pQPCPosition) {
			com.sun.jna.Function f = com.sun.jna.Function.getFunction(getVTablePointer(pThis, 3),
					com.sun.jna.Function.ALT_CONVENTION);
			int res = f.invokeInt(
					new Object[] { pThis, ppData, pNumFramesToRead, pdwFlags, pDevicePosition, pQPCPosition });
			return new HRESULT(res);
		}

		public static HRESULT IAudioCaptureClient_ReleaseBuffer(Pointer pThis, int NumFramesRead) {
			com.sun.jna.Function f = com.sun.jna.Function.getFunction(getVTablePointer(pThis, 4),
					com.sun.jna.Function.ALT_CONVENTION);
			int res = f.invokeInt(new Object[] { pThis, NumFramesRead });
			return new HRESULT(res);
		}

		public static HRESULT IAudioRenderClient_GetBuffer(Pointer pThis, int NumFramesRequested,
				PointerByReference ppData) {
			com.sun.jna.Function f = com.sun.jna.Function.getFunction(getVTablePointer(pThis, 3),
					com.sun.jna.Function.ALT_CONVENTION);
			int res = f.invokeInt(new Object[] { pThis, NumFramesRequested, ppData });
			return new HRESULT(res);
		}

		public static HRESULT IAudioRenderClient_ReleaseBuffer(Pointer pThis, int NumFramesWritten, int dwFlags) {
			com.sun.jna.Function f = com.sun.jna.Function.getFunction(getVTablePointer(pThis, 4),
					com.sun.jna.Function.ALT_CONVENTION);
			int res = f.invokeInt(new Object[] { pThis, NumFramesWritten, dwFlags });
			return new HRESULT(res);
		}
	}
}