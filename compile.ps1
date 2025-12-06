param(
    [string]$LibPath = "C:\Users\USUARIO\Desktop\openjfx-25.0.1_windows-x64_bin-sdk\javafx-sdk-25.0.1\lib"
)

# Prepares output folder and compiles the JavaFX application.
New-Item -ItemType Directory -Force -Path "out" | Out-Null
javac `
    --module-path $LibPath `
    --add-modules javafx.controls,javafx.fxml `
    -d out `
    App.java

# Copy static assets so they are available on the classpath when running.
Copy-Item -Path "styles.css" -Destination "out" -Force
