# Variables

The application-wide constants are in `src/com/oranbyte/screenrec/constants`. The table below lists the values currently defined there.

## AppConstant

| Variable | Type | Current value | Description |
| --- | --- | --- | --- |
| `APP_NAME` | `String` | `Screen Recorder` | Application name used by the UI and related messages. |
| `SAVE_LOCATION` | `String` | `C:\\Users\\Shubham\\Desktop` | Directory where timestamped MP4 recordings are initially written. Change this for another machine. |
| `APP_FONT` | `Font` | Arial, plain, 16 pt | Default font used by the Swing interface. |
| `FPS` | `int` | `50` | Default target video frame rate. The recorder limits runtime changes to 1-60 FPS. |
| `NEARBY_SCAN_TIMEOUT` | `int` | `5` | Nearby-device scan timeout value, in seconds. |
| `SNORE_TOAST_PATH` | `String` | Absolute path to `lib/snoretoast.exe` | Path resolved at runtime for Windows toast notifications. |

## Capture and recording enums

| Type | Values | Description |
| --- | --- | --- |
| `CaptureMode` | `RECTANGLE`, `WINDOW`, `ENTIRE_SCREEN` | Selects whether the user draws an area, selects a window under the pointer, or captures the full screen. |
| `RecordingMode` | `SCREENSHOT`, `VIDEO` | Selects a still-image capture or MP4 video capture. |
| `RecordingState` | `IDLE`, `SELECTING`, `READY`, `RECORDING`, `PAUSED` | Tracks the capture workflow and determines which controls are enabled. |
| `Icons` | Named icon resources | Maps UI actions such as start, stop, microphone, share, and playback to bundled image resources. |

## Color constants

`AppColors` contains the Swing palette: backgrounds and surfaces, primary and danger colors, text colors, borders, selection colors, gray shades, and transparent color. These values control the appearance of the application rather than capture behavior.

When changing `SAVE_LOCATION`, use a path that exists or can be created by the application and ensure the current user has write permission.
