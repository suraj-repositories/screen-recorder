# Screen Recorder

Screen Recorder is a Windows desktop application for capturing screenshots and screen recordings. It uses Java Swing for the main interface and JavaFX for video playback.

## What It Can Do

- Capture a selected rectangle, an application window, or the entire screen.
- Take screenshots and preview them in the built-in image viewer.
- Record video as MP4 with H.264 video and AAC audio.
- Include the mouse cursor in recordings when it can be captured by Windows.
- Pause, resume, and stop a recording while viewing the elapsed time.
- Capture microphone audio and Windows system audio independently, then mix enabled sources into the recording.
- Play completed recordings in the built-in video player.
- Save, copy, or share a completed screenshot or recording.

## Requirements

- Windows, for the current WASAPI and native Windows integration.
- Java 22 or later.
- JavaFX libraries, including the Swing integration used by the application.
- JNA and JNA Platform native libraries.
- Xuggler/Xuggle media libraries and their native dependencies.
- `lib/snoretoast.exe` if Windows toast notifications are required.

The project is currently Windows-first. The system-audio implementation uses WASAPI and should not be expected to work unchanged on macOS or Linux.

## Run From a Java IDE

1. Clone or open the repository in a Java IDE.
2. Configure the project with Java 22 or later.
3. Add the JavaFX, JNA, and Xuggler/Xuggle libraries required by the project.
4. Make sure the native libraries are available to the JVM.
5. Run `com.oranbyte.screenrec.Main`.

The application initializes JavaFX and then opens the Swing main window.

## First Capture

1. In the main window, choose `Screenshot` or `Video`.
2. Choose `Rectangle`, `Window`, or `Entire Screen`.
3. Click `New`.
4. Select the capture area. The window mode selects an available window under the pointer; rectangle mode lets you draw an area.
5. For a screenshot, finish the selection to open the image preview.
6. For a video, click `Start`. A countdown appears before capture begins.
7. Use pause/resume as needed, then click stop to finish the recording.

The recorder adjusts the selected dimensions to even values before encoding, which is required by the configured video encoder.

## Output Location

Recordings are currently written as timestamped MP4 files named like `Recording 2026-08-27 143015.mp4` in the directory configured by `AppConstant.SAVE_LOCATION`. In the current source configuration, that directory is `C:\Users\Shubham\Desktop`.

After a capture is complete, the main window exposes actions to save a copy, copy the file to the system clipboard, and open the sharing dialog.

## Troubleshooting

### No system audio

Confirm that Windows has an active playback device and that the application can access it. System audio is captured through WASAPI loopback and is Windows-specific.

### No microphone audio

Check that Windows has an available microphone and that microphone access is allowed. The microphone toggle can be changed while a recording is active.

### Recording does not start

Create a valid selection before pressing `Start`. The selected width and height must remain greater than zero after even-dimension adjustment.

### Sharing cannot find a device

Nearby sharing uses LocalSend discovery on the local network. Confirm that both devices are on the same network, then refresh the nearby-device list. Firewall rules or network isolation can prevent discovery.
