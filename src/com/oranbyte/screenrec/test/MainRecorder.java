package com.oranbyte.screenrec.test;

import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import com.oranbyte.screenrec.recorder.WasapiAudioSource;

public class MainRecorder {

	public static void main(String[] args) {
		File outputFile = new File("recorded_audio.mp3");
		int recordTimeSeconds = 10;

		// WasapiAudioSource.Mode.LOOPBACK captures system sound (speakers/headphones)
		// WasapiAudioSource.Mode.CAPTURE captures microphone input
		WasapiAudioSource audioSource = new WasapiAudioSource(WasapiAudioSource.Mode.LOOPBACK);

		Thread recorderThread = new Thread(() -> {
			try {
				// Get native WASAPI Audio Format
				AudioFormat nativeFormat = audioSource.getCaptureFormat();
				boolean isFloat = nativeFormat.getEncoding() == AudioFormat.Encoding.PCM_FLOAT;

				audioSource.start((int) nativeFormat.getSampleRate(), nativeFormat.getChannels());

				// mp3spi encoders prefer 16-bit PCM signed input format
				AudioFormat pcmFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, nativeFormat.getSampleRate(),
						16, nativeFormat.getChannels(), nativeFormat.getChannels() * 2, nativeFormat.getSampleRate(),
						false // Little endian
				);

				System.out.println("Recording started using WASAPI...");
				System.out.println("Output target: " + outputFile.getAbsolutePath());

				// Create custom stream that pulls from WASAPI source and handles float-to-int16
				// if necessary
				InputStream wasapiStream = new WasapiInputStream(audioSource, isFloat);
				AudioInputStream pcmAudioStream = new AudioInputStream(wasapiStream, pcmFormat,
						AudioSystem.NOT_SPECIFIED);

				// Write stream to file as MP3 using mp3spi provider
				// AudioFileFormat.Type("MP3", "mp3") is dynamically registered by mp3spi
				AudioFileFormat.Type mp3Type = new AudioFileFormat.Type("MP3", "mp3");
				AudioSystem.write(pcmAudioStream, mp3Type, outputFile);

			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		recorderThread.start();

		try {
			// Record for the specified duration
			Thread.sleep(recordTimeSeconds * 1000L);
		} catch (InterruptedException ignored) {
		}

		// Gracefully stop the WASAPI capture thread
		System.out.println("Stopping recording...");
		audioSource.stop();

		try {
			recorderThread.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		System.out.println("Done! MP3 file written to: " + outputFile.getAbsolutePath());
	}

	/**
	 * Converts raw byte data from WasapiAudioSource to standard 16-bit PCM bytes.
	 */
	private static class WasapiInputStream extends InputStream {
		private final WasapiAudioSource source;
		private final boolean isFloat;

		public WasapiInputStream(WasapiAudioSource source, boolean isFloat) {
			this.source = source;
			this.isFloat = isFloat;
		}

		@Override
		public int read() {
			byte[] b = new byte[1];
			int result = read(b, 0, 1);
			return result == -1 ? -1 : (b[0] & 0xFF);
		}

		@Override
		public int read(byte[] b, int off, int len) {
			try {
				if (!isFloat) {
					return source.read(b, off, len);
				}

				// If source stream uses PCM_FLOAT (32-bit), convert to 16-bit signed PCM
				int floatBytesToRead = len * 2;
				byte[] floatBuffer = new byte[floatBytesToRead];
				int bytesRead = source.read(floatBuffer, 0, floatBytesToRead);

				if (bytesRead <= 0) {
					return bytesRead;
				}

				ByteBuffer src = ByteBuffer.wrap(floatBuffer, 0, bytesRead).order(ByteOrder.LITTLE_ENDIAN);
				ByteBuffer dest = ByteBuffer.wrap(b, off, len).order(ByteOrder.LITTLE_ENDIAN);

				int samplesToConvert = bytesRead / 4;
				for (int i = 0; i < samplesToConvert; i++) {
					float sample = src.getFloat();
					// Clamp bounds [-1.0, 1.0] to prevent audio clipping distortion
					sample = Math.max(-1.0f, Math.min(1.0f, sample));
					short pcm16 = (short) (sample * 32767.0f);
					dest.putShort(pcm16);
				}

				return samplesToConvert * 2;

			} catch (Exception e) {
				return -1;
			}
		}
	}
}