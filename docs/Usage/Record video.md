# Record video

1. Open Screen Recorder.
2. Select `Video` mode using the camera/video switch.
3. Choose `Rectangle`, `Window`, or `Entire Screen`.
4. Click **New** and select the capture area.
5. Use the microphone button to enable or disable microphone capture.
6. Use the speaker button to enable or disable Windows system-audio capture.
7. Click **Start**. The countdown appears before recording begins.
8. Click **Pause** to pause capture, then click **Resume** to continue.
9. Click **Stop** to finish and encode the MP4.

The recording includes the mouse cursor when Windows makes the cursor image available. Paused time is excluded from the recording timestamps and elapsed time. Video dimensions are adjusted to even values for the configured encoder.

## Output

Recordings are written as timestamped MP4 files in the configured `SAVE_LOCATION`, currently the desktop path defined in `AppConstant`. When processing completes, the video opens in the built-in JavaFX media player.

See [Recording Controls](../Customize/Recording%20Controls.md) for the complete control behavior and [Record voice](Record%20voice.md) for microphone capture.
