package com.oranbyte.screenrec.test;

import java.io.FileOutputStream;

import com.oranbyte.screenrec.recorder.SystemAudioSource;
import com.oranbyte.screenrec.recorder.WasapiAudioSource;

public class Main {

	public static void main(String[] args) throws Exception {
		int sampleRate = 44100;
		int channels = 2;
		int sampleSizeInBytes = 2; // 16-bit PCM
		int bytesPerSecond = sampleRate * channels * sampleSizeInBytes;

		recordForDuration(new WasapiAudioSource(WasapiAudioSource.Mode.LOOPBACK), "system_audio.raw", 5, bytesPerSecond,
				sampleRate, channels);

		recordForDuration(new WasapiAudioSource(WasapiAudioSource.Mode.CAPTURE), "mic_audio.raw", 5, bytesPerSecond,
				sampleRate, channels);
	}

	private static void recordForDuration(SystemAudioSource source, String outputFile, int durationSeconds,
			int bytesPerSecond, int sampleRate, int channels) throws Exception {

		source.start(sampleRate, channels);

		try (FileOutputStream fos = new FileOutputStream(outputFile)) {
			byte[] buffer = new byte[4096];
			long targetBytes = (long) bytesPerSecond * durationSeconds;
			long totalBytesRead = 0;

			while (totalBytesRead < targetBytes) {
				int bytesToRead = (int) Math.min(buffer.length, targetBytes - totalBytesRead);
				int read = source.read(buffer, 0, bytesToRead);

				if (read < 0) {
					break;
				}

				if (read > 0) {
					fos.write(buffer, 0, read);
					totalBytesRead += read;
				}
			}
		} finally {
			source.stop();
		}
	}
}