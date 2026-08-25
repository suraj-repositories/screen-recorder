
## On way Features

1. sound listener -- done - for windows

	Windows
	Capture system audio via WASAPI (native API)
	Capture microphone via WASAPI
	macOS
	Capture via CoreAudio (often with BlackHole or ScreenCaptureKit)
	Linux
	Capture via PulseAudio/PipeWire
	
	-- NOTE - max volume before start recording...

2. fix video player -- done
3. count-down before starting video recorder -- done
4. add copy and save buttons after capture area -- done
5. add images on capture-mode selection -- done
6. add color picker --- decided and dedicated for next update
7. add selection size on time -- done
8. fix the video recording : large time issue -- done
9. update share system :  done

├── share
│   │
│   ├── FileShareManager.java
│   ├── FileShareProvider.java
│   ├── ShareDevice.java
│   ├── TransferListener.java
│   │
│   └── localsend
│       ├── LocalSendProvider.java
│       ├── LocalSendClient.java
│       ├── LocalSendServer.java
│       ├── LocalSendDiscovery.java
│       ├── LocalSendIdentity.java
│       ├── LocalSendDevice.java
│       ├── LocalSendFile.java
│       ├── LocalSendProtocol.java
│       ├── LocalSendServer.java
│       └── LocalSendSslContext.java


issues : 
1. file sharing at multiple locations
2. connection timeout
3. once sent - reset the nearby sharing 



