param(
    [string]$LibPath = "C:\Users\USUARIO\Desktop\openjfx-25.0.1_windows-x64_bin-sdk\javafx-sdk-25.0.1\lib"
)

# Runs the compiled JavaFX application from the out directory.
java `
    --module-path $LibPath `
    --add-modules javafx.controls,javafx.fxml `
    --enable-native-access=javafx.graphics `
    -cp out `
    App
