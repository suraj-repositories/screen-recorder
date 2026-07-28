package com.oranbyte.screenrec.recorder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class JavaSoundAudioSource implements SystemAudioSource {

	private TargetDataLine line;

	@Override
	public void start(int sampleRate, int channels) throws Exception {

		AudioFormat format = new AudioFormat(sampleRate, 16, channels, true, false);

		DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

		if (!AudioSystem.isLineSupported(info)) {
			throw new LineUnavailableException("Audio line not supported");
		}

		line = (TargetDataLine) AudioSystem.getLine(info);
		line.open(format);
		line.start();
	}

	@Override
	public int read(byte[] buffer, int offset, int length) {
		return line.read(buffer, offset, length);
	}

	@Override
	public void stop() {
		if (line != null) {
			line.stop();
			line.close();
		}
	}
}