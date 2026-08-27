# Technical Overview

## Application Flow

`com.oranbyte.screenrec.Main` initializes JavaFX with `JFXPanel`, then creates the Swing `MainFrame` on the Swing event-dispatch thread. The main window creates a selection frame and delegates capture-area selection to `DrawSelectRectangle`.

## Recording Pipeline

`ScreenRecorder` uses `java.awt.Robot` to capture frames from the selected `Rectangle`. Frames are converted to BGR images, the cursor image is drawn when available, and frames are encoded through Xuggler/Xuggle into an H.264 video stream.

The recorder maintains separate microphone and system-audio capture paths. Microphone samples come from a Java Sound `TargetDataLine`. System audio uses `WasapiAudioSource` in loopback mode through JNA and Windows COM/WASAPI calls. Audio is normalized to a 44.1 kHz, mono, 16-bit target format and mixed before being encoded as AAC.

Pause time is excluded from the recording timestamps and elapsed-time calculation. Capture and encoding run on dedicated worker threads, while the Swing UI remains responsive.

## Sharing Pipeline

The share UI uses `FileShareManager` and the `FileShareProvider` abstraction. The current nearby-sharing provider is LocalSend. It discovers devices on the local network, establishes an HTTPS session with the selected device, uploads the file, and reports progress through `TransferListener`.

The LocalSend server sanitizes incoming file names to the file-name component before resolving its download path. Transfers can be cancelled; the active HTTP client and session state are then reset.

## Main Dependencies

| Dependency | Purpose |
| --- | --- |
| Java Swing | Main desktop UI and controls |
| JavaFX | Video playback and media rendering |
| Xuggler/Xuggle | H.264/AAC media encoding |
| JNA | Windows native API access |
| WASAPI | Windows system-audio capture |
| LocalSend protocol | Nearby file sharing |
| Snoretoast | Windows notifications |

## Current Platform Boundary

The UI and core screen capture use Java APIs, but system-audio capture and several integration features depend on Windows. The documented setup should therefore be treated as Windows-specific until platform-specific audio sources and native integrations are added.