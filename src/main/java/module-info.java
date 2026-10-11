module com.example.autoclicker {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires com.github.kwhat.jnativehook;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens com.example.autoclicker to javafx.fxml;
    opens com.example.autoclicker.controller to javafx.fxml;
    opens com.example.autoclicker.DTO to com.fasterxml.jackson.databind;
    opens com.example.autoclicker.domain to com.fasterxml.jackson.databind;
    opens com.example.autoclicker.enums to com.fasterxml.jackson.databind;

    exports com.example.autoclicker;
    exports com.example.autoclicker.domain;
    exports com.example.autoclicker.DTO;
    exports com.example.autoclicker.enums;
}