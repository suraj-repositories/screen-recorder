package com.oranbyte.screenrec.audio;

import javax.sound.sampled.AudioFormat;

/**
 * plug-able abstraction
 */
public interface SystemAudioSource {

	/**
	 * Called once before capture begins.
	 *
	 * @param sampleRate desired sample rate, e.g. 44100
	 * @param channels   desired channel count, e.g. 1 for mono, 2 for stereo
	 */
	void start(int sampleRate, int channels) throws Exception;

	/**
	 * Blocking read of raw PCM bytes, same contract as
	 * {@link javax.sound.sampled.TargetDataLine#read(byte[], int, int)}.
	 *
	 * @return number of bytes actually read, or -1 if the source is closed
	 */
	int read(byte[] buffer, int offset, int length) throws Exception;

	/**
	 * Called once when capture stops. Must release any native resources.
	 */
	void stop();

	AudioFormat getCaptureFormat();
}