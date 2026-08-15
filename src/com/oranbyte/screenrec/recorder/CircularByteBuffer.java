package com.oranbyte.screenrec.recorder;

/**
 * Minimal blocking ring buffer used to decouple audio capture threads from
 * the mixer/encoder thread. {@link #read} zero-pads and returns after a
 * timeout if not enough data is available, so a stalled/slow source never
 * blocks the other source from being encoded.
 */
public class CircularByteBuffer {
	private final byte[] buffer;
	private int writePos = 0;
	private int readPos = 0;
	private int available = 0;
	private volatile boolean finished = false;
	private final Object lock = new Object();

	public CircularByteBuffer(int capacity) {
		this.buffer = new byte[Math.max(capacity, 4096)];
	}

	void write(byte[] data, int off, int len) {
		synchronized (lock) {
			int written = 0;
			while (written < len) {
				while (available == buffer.length && !finished) {
					try {
						lock.wait();
					} catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						return;
					}
				}
				if (finished) {
					return;
				}
				int spaceLeft = buffer.length - available;
				int chunk = Math.min(spaceLeft, len - written);
				for (int i = 0; i < chunk; i++) {
					buffer[writePos] = data[off + written + i];
					writePos = (writePos + 1) % buffer.length;
				}
				available += chunk;
				written += chunk;
				lock.notifyAll();
			}
		}
	}

 
	void read(byte[] out, int off, int len, long timeoutMs) {
		synchronized (lock) {
			long deadline = System.currentTimeMillis() + timeoutMs;
			while (available < len && !finished) {
				long remaining = deadline - System.currentTimeMillis();
				if (remaining <= 0) {
					break;
				}
				try {
					lock.wait(remaining);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}

			int toRead = Math.min(available, len);
			for (int i = 0; i < toRead; i++) {
				out[off + i] = buffer[readPos];
				readPos = (readPos + 1) % buffer.length;
			}
			available -= toRead;
			for (int i = toRead; i < len; i++) {
				out[off + i] = 0;
			}
			lock.notifyAll();
		}
	}

	void markFinished() {
		synchronized (lock) {
			finished = true;
			lock.notifyAll();
		}
	}
}
