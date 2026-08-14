package com.oranbyte.screenrec.test;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.File;
import java.io.InputStream;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

public class MainApp extends Application {

    private static final MethodHandle FIND_WINDOW;

    static {
        // Linker setup using Java 25 Foreign Function & Memory API
        Linker linker = Linker.nativeLinker();
        SymbolLookup user32 = SymbolLookup.libraryLookup("User32.dll", Arena.global());
        
        try {
            FIND_WINDOW = linker.downcallHandle(
                user32.find("FindWindowW").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to bind User32.dll methods natively", e);
        }
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("JavaFX Native Share Client");

        Button shareBtn = new Button("📤 Open Windows Share Dialog");
        shareBtn.setOnAction(event -> {
            // Target file provided
            File file = new File("C:\\Users\\Shubham\\Desktop\\sql.png");
            
            if (file.exists()) {
                triggerWindowsShare(primaryStage, file.toPath());
            } else {
                System.err.println("File error: Target path does not exist!");
            }
        });

        StackPane root = new StackPane(shareBtn);
        primaryStage.setScene(new Scene(root, 400, 250));
        primaryStage.show();
    }

    private void triggerWindowsShare(Stage stage, Path targetFilePath) {
        try (Arena arena = Arena.ofConfined()) {
            // 1. Temporarily swap titles to isolate this specific JavaFX window thread handle
            String originalTitle = stage.getTitle();
            String uniqueId = "JFX_WINDOW_" + UUID.randomUUID().toString();
            stage.setTitle(uniqueId);

            // 2. Allocate the wide-string token for User32 Interop engine mapping
            MemorySegment nativeTitleStr = arena.allocateFrom(uniqueId, java.nio.charset.StandardCharsets.UTF_16LE);
            MemorySegment hwnd = (MemorySegment) FIND_WINDOW.invokeExact(MemorySegment.NULL, nativeTitleStr);

            // Revert application window title back instantly
            stage.setTitle(originalTitle);

            if (hwnd.equals(MemorySegment.NULL)) {
                throw new RuntimeException("Native thread lookup failed: Unable to fetch Window HWND pointer.");
            }

            // 3. Extract the ShareHelper.exe out from application resource directory safely
            Path executionHelperBinary = extractShareHelper();

            // 4. Fire background process execution 
            System.out.println("Calling Share Helper for HWND Address: 0x" + Long.toHexString(hwnd.address()));
            new ProcessBuilder(
                executionHelperBinary.toAbsolutePath().toString(),
                targetFilePath.toAbsolutePath().toString(),
                String.valueOf(hwnd.address())
            ).start();

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private Path extractShareHelper() {
        try {
            Path tempDir = Path.of(System.getProperty("java.io.tmpdir"));
            Path destinationExe = tempDir.resolve("ShareHelper.exe");
 
            if (!Files.exists(destinationExe)) {
                try (InputStream binaryStream = MainApp.class.getResourceAsStream("/ShareHelper.exe")) {
                    if (binaryStream == null) {
                        throw new RuntimeException("ShareHelper.exe is missing from your src/main/resources folder!");
                    }
                    Files.copy(binaryStream, destinationExe, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return destinationExe;
        } catch (Exception e) {
            throw new RuntimeException("Resource extraction execution routine terminated abnormally", e);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
