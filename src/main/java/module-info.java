module com.example.autoclicker {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.example.autoclicker to javafx.fxml;
    opens com.example.autoclicker.Controller to javafx.fxml;
    exports com.example.autoclicker;
}