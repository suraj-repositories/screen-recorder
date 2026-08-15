package com.oranbyte.screenrec.share;

public interface TransferListener {

	default void onStarted(long totalBytes) {
	}

	default void onProgress(long transferredBytes, long totalBytes) {
	}

	default void onCompleted() {
	}

	default void onFailed(Exception exception) {
	}

	default void onCancelled() {
	}
}