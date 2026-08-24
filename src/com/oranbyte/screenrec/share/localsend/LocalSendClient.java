package com.oranbyte.screenrec.share.localsend;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ssl.SSLContext;

import org.json.JSONObject;

import com.oranbyte.screenrec.share.TransferListener;

public class LocalSendClient {

	private final AtomicBoolean cancelled = new AtomicBoolean(false);

	private volatile String currentSessionId;
	private volatile LocalSendDevice currentDevice;

	public void send(LocalSendDevice device, LocalSendFile file, TransferListener listener) throws Exception {

		cancelled.set(false);
		currentDevice = device;

		JSONObject request = createPrepareRequest(file);
		HttpClient client = createHttpClient(device);

		URI uri = URI.create(device.getBaseUrl() + LocalSendProtocol.PREPARE_UPLOAD_PATH);

		HttpRequest requestMessage = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofMinutes(2))
				.header(LocalSendProtocol.HEADER_CONTENT_TYPE, LocalSendProtocol.CONTENT_TYPE_JSON)
				.POST(HttpRequest.BodyPublishers.ofString(request.toString()))
				.build();

		try {
			HttpResponse<String> response = client.send(requestMessage, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() == 204) {
				listener.onStarted(file.getSize());
				listener.onProgress(file.getSize(), file.getSize());
				listener.onCompleted();
				return;
			}

			// Handle LocalSend readiness / response status codes
			if (response.statusCode() == 403) {
				throw new IOException("Transfer rejected by remote device.");
			} else if (response.statusCode() == 409) {
				throw new IOException("Remote device is busy with another transfer.");
			} else if (response.statusCode() == 401) {
				throw new IOException("Remote device requires PIN authentication.");
			} else if (response.statusCode() != 200) {
				throw new IOException("prepare-upload failed. HTTP " + response.statusCode() + ": " + response.body());
			}

			JSONObject result = new JSONObject(response.body());
			currentSessionId = result.getString("sessionId");
			JSONObject files = result.getJSONObject("files");
			String token = files.getString(file.getId());

			upload(client, device, file, currentSessionId, token, listener);

		} catch (Exception e) {
			if (cancelled.get()) {
				listener.onCancelled();
			} else {
				listener.onFailed(e);
				throw e;
			}
		} finally {
			resetSession();
		}
	}

	private JSONObject createPrepareRequest(LocalSendFile file) throws IOException {

		JSONObject root = new JSONObject();
		JSONObject info = createIdentityJson();

		root.put("info", info);

		JSONObject files = new JSONObject();
		files.put(file.getId(), file.toJson(true));
		root.put("files", files);

		return root;
	}

	private JSONObject createIdentityJson() {

		JSONObject info = new JSONObject();
		info.put("alias", LocalSendIdentity.getAlias());
		info.put("version", LocalSendProtocol.VERSION);
		info.put("deviceModel", LocalSendIdentity.getDeviceModel());
		info.put("deviceType", LocalSendIdentity.getDeviceType());
		info.put("fingerprint", LocalSendIdentity.getFingerprint());
		info.put("port", LocalSendProtocol.DEFAULT_PORT);
		info.put("protocol", LocalSendProtocol.PROTOCOL_HTTPS);
		info.put("download", false);

		return info;
	}

	private void upload(HttpClient client, LocalSendDevice device, LocalSendFile file, String sessionId, String token,
			TransferListener listener) throws Exception {

		listener.onStarted(file.getSize());

		String uriString = device.getBaseUrl() + LocalSendProtocol.UPLOAD_PATH + "?sessionId=" + encode(sessionId)
				+ "&fileId=" + encode(file.getId()) + "&token=" + encode(token);

		HttpRequest request = HttpRequest.newBuilder(URI.create(uriString))
				.timeout(Duration.ofHours(2))
				.header(LocalSendProtocol.HEADER_CONTENT_TYPE, LocalSendProtocol.CONTENT_TYPE_BINARY)
				.POST(new FileBodyPublisher(file, listener, cancelled))
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		if (cancelled.get()) {
			listener.onCancelled();
			return;
		}

		if (response.statusCode() != 200) {
			throw new IOException("Upload failed. HTTP " + response.statusCode() + ": " + response.body());
		}

		listener.onProgress(file.getSize(), file.getSize());
		listener.onCompleted();
	}

	public void cancelCurrentTransfer() {

		cancelled.set(true);

		String session = currentSessionId;
		LocalSendDevice device = currentDevice;

		if (session == null || device == null) {
			return;
		}

		try {
			HttpClient client = createHttpClient(device);
			URI uri = URI.create(device.getBaseUrl() + LocalSendProtocol.CANCEL_PATH + "?sessionId=" + encode(session));

			HttpRequest request = HttpRequest.newBuilder(uri)
					.timeout(Duration.ofSeconds(10))
					.POST(HttpRequest.BodyPublishers.noBody())
					.build();

			client.sendAsync(request, HttpResponse.BodyHandlers.discarding());
		} catch (Exception ignored) {
		} finally {
			resetSession();
		}
	}

	private synchronized void resetSession() {
		currentSessionId = null;
		currentDevice = null;
	}

	private HttpClient createHttpClient(LocalSendDevice device) throws Exception {

		HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10));

		if (LocalSendProtocol.PROTOCOL_HTTPS.equalsIgnoreCase(device.getProtocol())) {
			SSLContext sslContext = LocalSendSslContext.createClientContext(device.getFingerprint());
			builder.sslContext(sslContext);
		}

		return builder.build();
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static class FileBodyPublisher implements HttpRequest.BodyPublisher {

		private final LocalSendFile file;
		private final TransferListener listener;
		private final AtomicBoolean cancelled;

		FileBodyPublisher(LocalSendFile file, TransferListener listener, AtomicBoolean cancelled) {
			this.file = file;
			this.listener = listener;
			this.cancelled = cancelled;
		}

		@Override
		public long contentLength() {
			return file.getSize();
		}

		@Override
		public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {

			subscriber.onSubscribe(new Flow.Subscription() {

				private InputStream input;
				private long transferred = 0;
				private boolean completed = false;

				@Override
				public synchronized void request(long n) {

					if (completed || cancelled.get()) {
						return;
					}

					if (n <= 0) {
						subscriber.onError(new IllegalArgumentException("Invalid demand: " + n));
						return;
					}

					try {
						if (input == null) {
							input = Files.newInputStream(file.getFile().toPath());
						}

						byte[] buffer = new byte[1024 * 1024]; // 1MB buffer
						long requested = n;

						while (requested > 0 && !completed && !cancelled.get()) {
							int read = input.read(buffer);

							if (read == -1) {
								completed = true;
								input.close();
								subscriber.onComplete();
								break;
							}

							byte[] data = Arrays.copyOf(buffer, read);
							subscriber.onNext(ByteBuffer.wrap(data));
							transferred += read;
							listener.onProgress(transferred, file.getSize());

							requested--;
						}

					} catch (Exception e) {
						completed = true;
						try {
							if (input != null) input.close();
						} catch (IOException ignored) {}
						subscriber.onError(e);
					}
				}

				@Override
				public synchronized void cancel() {
					cancelled.set(true);
					completed = true;
					try {
						if (input != null) input.close();
					} catch (IOException ignored) {}
				}
			});
		}
	}
}