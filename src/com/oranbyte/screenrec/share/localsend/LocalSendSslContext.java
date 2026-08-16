package com.oranbyte.screenrec.share.localsend;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HexFormat;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public final class LocalSendSslContext {

	private static final char[] PASSWORD = "screenrecorder".toCharArray();

	private static final String KEY_ALIAS = "localsend";

	private static final Path KEYSTORE = Path.of(System.getProperty("user.home"), ".oranbyte", "screenrecorder",
			"localsend.p12");

	private LocalSendSslContext() {
	}

	public static SSLContext createServerContext() throws Exception {

		ensureCertificate();

		KeyStore keyStore = loadKeyStore();

		KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

		factory.init(keyStore, PASSWORD);

		SSLContext context = SSLContext.getInstance("TLS");

		context.init(factory.getKeyManagers(), null, new SecureRandom());

		return context;
	}

	public static SSLContext createClientContext(String expectedFingerprint) throws Exception {

		X509TrustManager trustManager = new FingerprintTrustManager(expectedFingerprint);

		SSLContext context = SSLContext.getInstance("TLS");

		context.init(null, new X509TrustManager[] { trustManager }, new SecureRandom());

		return context;
	}

	public static String getCertificateFingerprint() throws Exception {

		ensureCertificate();

		KeyStore keyStore = loadKeyStore();

		Certificate certificate = keyStore.getCertificate(KEY_ALIAS);

		return sha256(certificate.getEncoded());
	}

	private static KeyStore loadKeyStore() throws Exception {

		KeyStore keyStore = KeyStore.getInstance("PKCS12");

		try (InputStream input = Files.newInputStream(KEYSTORE)) {

			keyStore.load(input, PASSWORD);
		}

		return keyStore;
	}

	private static synchronized void ensureCertificate() throws Exception {

		if (Files.exists(KEYSTORE)) {
			return;
		}

		Files.createDirectories(KEYSTORE.getParent());

		if (Security.getProvider("BC") == null) {

			Security.addProvider(new BouncyCastleProvider());
		}

		KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");

		generator.initialize(2048, new SecureRandom());

		KeyPair keyPair = generator.generateKeyPair();

		Date notBefore = new Date(System.currentTimeMillis() - 60_000);

		Date notAfter = new Date(System.currentTimeMillis() + 3650L * 24 * 60 * 60 * 1000);

		BigInteger serial = new BigInteger(128, new SecureRandom());

		X500Name name = new X500Name("CN=ScreenRecorder");

		JcaX509v3CertificateBuilder builder = new JcaX509v3CertificateBuilder(name, serial, notBefore, notAfter, name,
				keyPair.getPublic());

		ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").setProvider("BC")
				.build(keyPair.getPrivate());

		X509CertificateHolder holder = builder.build(signer);

		X509Certificate certificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(holder);

		certificate.verify(keyPair.getPublic());

		KeyStore keyStore = KeyStore.getInstance("PKCS12");

		keyStore.load(null, PASSWORD);

		keyStore.setKeyEntry(KEY_ALIAS, keyPair.getPrivate(), PASSWORD, new Certificate[] { certificate });

		try (OutputStream output = Files.newOutputStream(KEYSTORE, StandardOpenOption.CREATE,
				StandardOpenOption.TRUNCATE_EXISTING)) {

			keyStore.store(output, PASSWORD);
		}
	}

	private static String sha256(byte[] data) throws Exception {

		MessageDigest digest = MessageDigest.getInstance("SHA-256");

		return HexFormat.of().formatHex(digest.digest(data));
	}

	private static class FingerprintTrustManager implements X509TrustManager {

		private final String expected;

		FingerprintTrustManager(String expected) {

			this.expected = normalize(expected);
		}

		@Override
		public void checkClientTrusted(X509Certificate[] chain, String authType) {
		}

		@Override
		public void checkServerTrusted(X509Certificate[] chain, String authType)
				throws java.security.cert.CertificateException {

			if (chain == null || chain.length == 0) {

				throw new java.security.cert.CertificateException("No certificate received.");
			}

			try {

				String actual = normalize(sha256(chain[0].getEncoded()));

				if (!actual.equals(expected)) {

					throw new java.security.cert.CertificateException(
							"LocalSend certificate " + "fingerprint mismatch.");
				}

			} catch (java.security.cert.CertificateException e) {

				throw e;

			} catch (Exception e) {

				throw new java.security.cert.CertificateException("Unable to verify " + "certificate.", e);
			}
		}

		@Override
		public X509Certificate[] getAcceptedIssuers() {

			return new X509Certificate[0];
		}

		private static String normalize(String value) {

			if (value == null) {
				return "";
			}

			return value.replace(":", "").replace(" ", "").toLowerCase();
		}
	}
}