## JavaFX setup

- SDK local path: `C:\Users\USUARIO\Desktop\openjfx-25.0.1_windows-x64_bin-sdk\javafx-sdk-25.0.1\lib`
- Compile: `.\compile.ps1` (uses the path above by default; override with `.\compile.ps1 -LibPath "C:\ruta\a\lib"`)
- Run: `.\run.ps1` (same override flag if needed)
- If `javac` or `java` are not on PATH, set `JAVA_HOME` to a JDK 21+ and add `%JAVA_HOME%\bin`.

The project uses the classpath approach (no module-info.java). Both scripts pass `--module-path` and `--add-modules javafx.controls,javafx.fxml` so JavaFX is available to the app. The run script also includes `--enable-native-access=javafx.graphics` to silence the JavaFX native library warning shown on newer JDKs.
