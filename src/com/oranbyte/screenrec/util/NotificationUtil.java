package com.oranbyte.screenrec.util;

import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;

import com.oranbyte.screenrec.constants.AppConstant;
import com.oranbyte.screenrec.constants.Icons;

public class NotificationUtil {

	public interface NotificationClickListener {
		void onClick();
	}

	private static String snoreToastPath = AppConstant.SNORE_TOAST_PATH;
	private static String appId = AppConstant.APP_NAME;
	private static final String PIPE_NAME = "OranByte.ScreenRecorder";

	private static TrayIcon trayIcon;
	private static ActionListener trayClickListener;

	private static final String[] VIDEO_EXTENSIONS = { "mp4", "mkv", "avi", "mov", "webm" };
	private static final int BANNER_WIDTH = 364;
	private static final int BANNER_HEIGHT = 180;

	private static final Map<String, Runnable> actions = new ConcurrentHashMap<>();

	static {
		if (SystemTray.isSupported()) {
			try {
				SystemTray tray = SystemTray.getSystemTray();
				trayIcon = new TrayIcon(Icons.FAVICON.icon().getImage(), AppConstant.APP_NAME);
				trayIcon.setImageAutoSize(true);
				tray.add(trayIcon);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void initializeActions() {
		new NamedPipeListener(PIPE_NAME, () -> System.out.println("Notification clicked!")).start();
	}

	public static void setSnoreToastPath(String path) {
		snoreToastPath = path;
	}

	public static void setAppId(String id) {
		appId = id;
	}

	public static void notify(String title, String message) {
		notify(title, message, (NotificationClickListener) null);
	}

	public static void notify(String title, String message, NotificationClickListener onClick) {
		showToast(title, message, null, onClick);
	}

	public static void notify(String title, String message, File imageFile) {
		notify(title, message, imageFile, null);
	}

	public static void notify(String title, String message, File imageFile, NotificationClickListener onClick) {
		if (imageFile == null || !imageFile.exists()) {
			notify(title, message, onClick);
			return;
		}
		if (isVideoExtension(getExtension(imageFile))) {
			notifyVideo(title, message, imageFile, onClick);
			return;
		}
		File toastImage = prepareToastImage(imageFile);
		showToast(title, message, toastImage, onClick);
	}

	public static void notifyVideo(String title, String message, File videoFile) {
		notifyVideo(title, message, videoFile, null);
	}

	public static void notifyVideo(String title, String message, File videoFile, NotificationClickListener onClick) {

		if (videoFile == null || !videoFile.exists()) {
			notify(title, message, onClick);
			return;
		}

		new Thread(() -> {
			File frame = extractVideoThumbnail(videoFile);
			File img = frame != null ? prepareToastImage(frame) : Icons.PLAY_VIDEO_CIRCLE.file();
			showToast(title, message, img, onClick);
		}, "VideoNotification").start();
	}

	private static void showToast(String title, String message, File image, NotificationClickListener onClick) {
		if (isSnoreToastAvailable()) {
			try {
				runSnoreToast(title, message, image, onClick);
			} catch (IOException e) {
				showTrayFallback(title, message, onClick);
			}
		} else {
			showTrayFallback(title, message, onClick);
		}
	}

	private static boolean isSnoreToastAvailable() {
		try {
			Process p = new ProcessBuilder(snoreToastPath, "-v").start();
			p.getInputStream().transferTo(OutputStream.nullOutputStream());
			return p.waitFor() == 0 || p.waitFor() >= -1;
		} catch (IOException | InterruptedException e) {
			return false;
		}
	}

	private static void runSnoreToast(String title, String message, File image, NotificationClickListener onClick)
			throws IOException {
		String actionId = UUID.randomUUID().toString();

//		actions.put(actionId, onClick);

		List<String> args = new ArrayList<>();

		args.add(snoreToastPath);

		args.add("-t");
		args.add(title);

		args.add("-m");
		args.add(message);

		args.add("-appID");
		args.add(appId);

		if (image != null) {
			args.add("-p");
			args.add(image.getAbsolutePath());
		}

		args.add("-pipeName");
		args.add("\\\\.\\pipe\\" + PIPE_NAME);

		args.add("-id");
		args.add(actionId);

		new ProcessBuilder(args).start();
	}

	private static void showTrayFallback(String title, String message, NotificationClickListener onClick) {
		if (trayIcon == null) {
			return;
		}
		if (trayClickListener != null) {
			trayIcon.removeActionListener(trayClickListener);
		}
		if (onClick != null) {
			trayClickListener = e -> onClick.onClick();
			trayIcon.addActionListener(trayClickListener);
		}

		trayIcon.displayMessage(AppConstant.APP_NAME, buildBody(title, message), TrayIcon.MessageType.INFO);
	}

	private static String buildBody(String title, String message) {
		if (title == null || title.isBlank()) {
			return message == null ? "" : message;
		}
		if (message == null || message.isBlank()) {
			return title;
		}
		return title + "\n" + message;
	}

	private static String getExtension(File file) {
		String name = file.getName();
		int dot = name.lastIndexOf('.');
		return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
	}

	private static boolean isVideoExtension(String ext) {
		for (String v : VIDEO_EXTENSIONS) {
			if (v.equals(ext)) {
				return true;
			}
		}
		return false;
	}

	private static File extractVideoThumbnail(File videoFile) {
		try {
			File tempThumb = File.createTempFile("thumb_", ".png");
			tempThumb.deleteOnExit();

			Process process = new ProcessBuilder("ffmpeg", "-y", "-ss", "00:00:01", "-i", videoFile.getAbsolutePath(),
					"-frames:v", "1", "-vf", "scale=" + BANNER_WIDTH + ":" + BANNER_HEIGHT, tempThumb.getAbsolutePath())
					.start();
			process.getInputStream().transferTo(OutputStream.nullOutputStream());
			int exit = process.waitFor();

			if (exit == 0 && tempThumb.exists() && Files.size(tempThumb.toPath()) > 0) {
				return tempThumb;
			}
			tempThumb.delete();
			return null;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	private static File prepareToastImage(File sourceImage) {
		try {
			BufferedImage img = ImageIO.read(sourceImage);
			if (img == null) {
				return null;
			}

			int w = img.getWidth();
			int h = img.getHeight();
			double scale = Math.max((double) BANNER_WIDTH / w, (double) BANNER_HEIGHT / h);
			int scaledW = (int) Math.ceil(w * scale);
			int scaledH = (int) Math.ceil(h * scale);

			BufferedImage scaled = new BufferedImage(scaledW, scaledH, BufferedImage.TYPE_INT_ARGB);
			var scaleG = scaled.createGraphics();
			scaleG.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
					java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			scaleG.drawImage(img, 0, 0, scaledW, scaledH, null);
			scaleG.dispose();

			int cropX = Math.max(0, (scaledW - BANNER_WIDTH) / 2);
			int cropY = Math.max(0, (scaledH - BANNER_HEIGHT) / 2);
			BufferedImage cropped = scaled.getSubimage(cropX, cropY, BANNER_WIDTH, BANNER_HEIGHT);

			File out = File.createTempFile("toast_", ".png");
			out.deleteOnExit();
			ImageIO.write(cropped, "png", out);
			return out;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}