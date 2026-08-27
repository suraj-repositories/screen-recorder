# Setup in Eclipse

## Import the project

1. Install a 64-bit JDK 22 or later and Eclipse IDE for Java Developers.
2. Download or clone the repository.
3. In Eclipse, select **File > Import > General > Existing Projects into Workspace**.
4. Choose the repository directory and finish the import.
5. If Eclipse does not recognize the project automatically, create a Java project using the repository directory as its location and add the existing `src` folder as a source folder.

## Configure Java

1. Open **Window > Preferences > Java > Installed JREs**.
2. Add and select the JDK 22 installation.
3. Open the project properties and set **Java Compiler > Compiler compliance level** to 22 or the installed compatible level.
4. Make sure the project uses the Java module path when Eclipse asks whether a dependency belongs on the classpath or module path.

## Add libraries

Add JavaFX, JNA, Xuggler/Xuggle, and their native dependencies as described in [Dependencies](Dependencies.md). Add JavaFX modules required by the code, including the Swing integration and media modules.

If the project contains a `module-info.java`, make sure the required modules are available to the module path and that native libraries are discoverable by the JVM. Check Eclipse's **Problems** view after refreshing the project.

## Run the application

1. Open `src/com/oranbyte/screenrec/Main.java`.
2. Right-click the file and select **Run As > Java Application**.
3. Grant Windows microphone access if microphone capture is needed.
4. If notifications are enabled, verify that `lib/snoretoast.exe` is present.

The main window should open after JavaFX is initialized. For the first capture, see [Introduction](Introduction.md) and the usage pages.
