package com.oranbyte.screenrec.share.localsend;

import com.oranbyte.screenrec.constants.AppConstant;

public final class LocalSendProtocol {

	private LocalSendProtocol() {
	}
	
	public static final String ALIAS = AppConstant.APP_NAME;
	
	public static final String DEVICE_MODEL = System.getProperty("os.name");

	public static final String VERSION = "2.0";

	public static final String MULTICAST_ADDRESS = "224.0.0.167";

	public static final int DEFAULT_PORT = 53317;

	public static final String BASE_PATH = "/api/localsend/v2";

	public static final String REGISTER_PATH = BASE_PATH + "/register";

	public static final String PREPARE_UPLOAD_PATH = BASE_PATH + "/prepare-upload";

	public static final String UPLOAD_PATH = BASE_PATH + "/upload";

	public static final String CANCEL_PATH = BASE_PATH + "/cancel";

	public static final String INFO_PATH = BASE_PATH + "/info";

	public static final String PROTOCOL_HTTP = "http";

	public static final String PROTOCOL_HTTPS = "https";

	public static final String DEVICE_TYPE_DESKTOP = "desktop";

	public static final String DEVICE_TYPE_MOBILE = "mobile";

	public static final String DEVICE_TYPE_WEB = "web";

	public static final String DEVICE_TYPE_HEADLESS = "headless";

	public static final String DEVICE_TYPE_SERVER = "server";

	public static final String HEADER_CONTENT_TYPE = "Content-Type";

	public static final String CONTENT_TYPE_JSON = "application/json";

	public static final String CONTENT_TYPE_BINARY = "application/octet-stream";
}