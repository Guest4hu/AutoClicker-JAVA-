module com.example.autoclicker {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires com.github.kwhat.jnativehook;


    opens com.example.autoclicker to javafx.fxml;
    opens com.example.autoclicker.controller to javafx.fxml;
    exports com.example.autoclicker;
}