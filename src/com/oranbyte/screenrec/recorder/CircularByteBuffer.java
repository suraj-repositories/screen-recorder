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
	        if (len > buffer.length) {
	            off += (len - buffer.length);
	            len = buffer.length;
	        }
	        int spaceLeft = buffer.length - available;
	        if (len > spaceLeft) {
	            int toEvict = len - spaceLeft;
	            readPos = (readPos + toEvict) % buffer.length;
	            available -= toEvict;
	        }
	        for (int i = 0; i < len; i++) {
	            buffer[writePos] = data[off + i];
	            writePos = (writePos + 1) % buffer.length;
	        }
	        available += len;
	        lock.notifyAll();
	    }
	} 
	
	
	int read(byte[] out, int off, int len, long timeoutMs) {
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

	        return toRead;
	    }
	}
	
	int availableBytes() {
	    synchronized (lock) {
	        return available;
	    }
	}

	
	void skip(int n) {
	    synchronized (lock) {
	        int toSkip = Math.min(n, available);
	        readPos = (readPos + toSkip) % buffer.length;
	        available -= toSkip;
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
