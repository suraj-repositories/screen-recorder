package com.oranbyte.screenrec.share.localsend;

import java.security.KeyStore;

public final class LocalSendIdentity {

	private static final String ALIAS = LocalSendProtocol.ALIAS;
	private static final String DEVICE_MODEL = LocalSendProtocol.DEVICE_MODEL;
	private static final String DEVICE_TYPE = LocalSendProtocol.DEVICE_TYPE_DESKTOP;

	private LocalSendIdentity() {
	}

	public static String getFingerprint() {
		try {
			return LocalSendSslContext.getCertificateFingerprint();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to compute LocalSend fingerprint.", e);
		}
	}

	public static KeyStore getKeyStore() {
		try {
			return LocalSendSslContext.getKeyStore();
		} catch (Exception e) {
			throw new IllegalStateException("Unable to load LocalSend identity keystore.", e);
		}
	}

	public static char[] getKeyStorePassword() {
		return LocalSendSslContext.getPassword();
	}

	public static String getKeyAlias() {
		return LocalSendSslContext.getKeyAlias();
	}

	public static String getAlias() {
		return ALIAS;
	}

	public static String getDeviceModel() {
		return DEVICE_MODEL;
	}

	public static String getDeviceType() {
		return DEVICE_TYPE;
	}

	public static String getVersion() {
		return LocalSendProtocol.VERSION;
	}
}