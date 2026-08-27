# Dependencies

The project uses Java libraries and Windows-native components. Check the repository's build configuration, if added later, for exact versions and artifact coordinates.

| Dependency | Purpose | Link |
| --- | --- | --- |
| Java Development Kit 22+ | Compiles and runs the application. | [OpenJDK](https://jdk.java.net/) |
| JavaFX | Video playback and Swing/JavaFX integration. | [OpenJFX](https://openjfx.io/) |
| JNA and JNA Platform | Accesses Windows native APIs from Java. | [JNA on GitHub](https://github.com/java-native-access/jna) |
| Xuggler / Xuggle | Encodes video and audio streams, including H.264 and AAC. | [Xuggler](https://www.xuggle.com/xuggler/) |
| WASAPI | Captures Windows system audio through loopback. | [Microsoft WASAPI documentation](https://learn.microsoft.com/en-us/windows/win32/coreaudio/wasapi) |
| LocalSend protocol | Discovers nearby devices and transfers files over the local network. | [LocalSend](https://localsend.org/) |
| Snoretoast | Displays Windows toast notifications. | [Snoretoast on GitHub](https://github.com/GuiltyDolphin/snoretoast) |

## Native library notes

JavaFX, JNA, and Xuggler/Xuggle may require platform-specific native binaries. Use binaries matching the operating system architecture and the selected JDK. WASAPI and the current native integration are Windows-specific.

The application resolves the notification executable from `lib/snoretoast.exe`. Keep that file in the repository's `lib` directory when Windows toast notifications are required.
