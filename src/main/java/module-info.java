module com.example.autoclicker {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;
    requires com.github.kwhat.jnativehook;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;

    opens com.example.autoclicker to javafx.fxml;
    opens com.example.autoclicker.ui.controller to javafx.fxml;
    opens com.example.autoclicker.dto to com.fasterxml.jackson.databind;
    opens com.example.autoclicker.domain.model to com.fasterxml.jackson.databind;

    exports com.example.autoclicker;
    exports com.example.autoclicker.domain.model;
    exports com.example.autoclicker.domain.engine;
    exports com.example.autoclicker.dto;
    exports com.example.autoclicker.input.simulator;
    exports com.example.autoclicker.input.hotkey;
    exports com.example.autoclicker.input.window;
    exports com.example.autoclicker.input.macro;
    exports com.example.autoclicker.storage;
    exports com.example.autoclicker.ui.controller;
    exports com.example.autoclicker.ui.dialog;
}