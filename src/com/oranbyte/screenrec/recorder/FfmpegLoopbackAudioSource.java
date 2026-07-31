package com.oranbyte.screenrec.recorder;

import java.io.InputStream;

/**
 * Cross-platform fallback that shells out to FFmpeg for the actual capture and
 * reads raw signed 16-bit little-endian PCM off its stdout. This avoids writing
 * native COM/CoreAudio code yourself, at the cost of requiring an ffmpeg binary
 * on PATH (or bundled with your app) and, on Windows, still requires a
 * loopback-capable input:
 *
 * - Windows: a virtual cable driver exposed to dshow (e.g.
 * "virtual-audio-capturer", or VB-CABLE's "CABLE Output"), OR a "Stereo Mix"
 * recording device if the sound driver exposes one. - macOS: avfoundation
 * reading from a virtual device like BlackHole, selected as an audio input
 * index. - Linux: pulse, reading directly from a sink's ".monitor" source - no
 * virtual device needed, Pulse exposes this natively.
 *
 * Adjust ffmpegArgsFor(...) below to match the device name/index on the machine
 * you're deploying to; device names in particular are not portable across
 * machines and should ideally be user-configurable rather than hardcoded.
 */
public class FfmpegLoopbackAudioSource implements SystemAudioSource {

	public enum Platform {
		WINDOWS, MACOS, LINUX
	}

	private final Platform platform;
	private final String deviceName;
	private Process process;
	private InputStream pcmStream;

	/**
	 * @param platform   which ffmpeg input format to use
	 * @param deviceName platform-specific device identifier, e.g.
	 *                   "virtual-audio-capturer" (Windows dshow), ":2" for an
	 *                   avfoundation input index (macOS),
	 *                   "alsa_output.pci-0000_00_1f.3.analog-stereo.monitor" (Linux
	 *                   pulse)
	 */
	public FfmpegLoopbackAudioSource(Platform platform, String deviceName) {
		this.platform = platform;
		this.deviceName = deviceName;
	}

	@Override
	public void start(int sampleRate, int channels) throws Exception {
		String[] command = ffmpegArgsFor(sampleRate, channels);

		ProcessBuilder pb = new ProcessBuilder(command);
		pb.redirectErrorStream(false); // keep ffmpeg's logging off stdout so it doesn't corrupt the PCM stream
		process = pb.start();
		pcmStream = process.getInputStream();

		// Drain stderr on a daemon thread so ffmpeg never blocks on a full pipe buffer.
		Thread stderrDrain = new Thread(() -> {
			try {
				process.getErrorStream().transferTo(java.io.OutputStream.nullOutputStream());
			} catch (Exception ignored) {
				// process exited; nothing to drain anymore
			}
		}, "ffmpeg-stderr-drain");
		stderrDrain.setDaemon(true);
		stderrDrain.start();
	}

	@Override
	public int read(byte[] buffer, int offset, int length) throws Exception {
		if (pcmStream == null) {
			return -1;
		}
		return pcmStream.read(buffer, offset, length);
	}

	@Override
	public void stop() {
		if (process != null) {
			process.destroy();
			try {
				process.waitFor(2, java.util.concurrent.TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			if (process.isAlive()) {
				process.destroyForcibly();
			}
		}
	}

	private String[] ffmpegArgsFor(int sampleRate, int channels) {
		switch (platform) {
		case WINDOWS:
			return new String[] { "ffmpeg", "-hide_banner", "-loglevel", "error", "-f", "dshow", "-i",
					"audio=" + deviceName, "-ar", String.valueOf(sampleRate), "-ac", String.valueOf(channels), "-f",
					"s16le", "pipe:1" };
		case MACOS:
			return new String[] { "ffmpeg", "-hide_banner", "-loglevel", "error", "-f", "avfoundation", "-i",
					deviceName, // e.g. ":2" - run `ffmpeg -f avfoundation -list_devices true -i ""` to find the
								// index
					"-ar", String.valueOf(sampleRate), "-ac", String.valueOf(channels), "-f", "s16le", "pipe:1" };
		case LINUX:
			return new String[] { "ffmpeg", "-hide_banner", "-loglevel", "error", "-f", "pulse", "-i", deviceName,
					"-ar", String.valueOf(sampleRate), "-ac", String.valueOf(channels), "-f", "s16le", "pipe:1" };
		default:
			throw new IllegalStateException("Unsupported platform: " + platform);
		}
	}
}