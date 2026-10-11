package com.example.autoclicker;

import com.example.autoclicker.controller.AutoClickerController;
import com.example.autoclicker.domain.AutoClickerEngine;
import com.example.autoclicker.domain.MacroEngine;
import com.example.autoclicker.service.DialogService;
import com.example.autoclicker.service.HotkeyService;
import com.example.autoclicker.service.MacroService;
import com.example.autoclicker.service.MacroStorageService;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;

public class AutoClickerApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException, NativeHookException {
        // Registra o hook global de teclado
        GlobalScreen.registerNativeHook();

        // Instancia o serviço de atalhos
        HotkeyService hotkeyService = new HotkeyService();
        MacroService macroService = new MacroService();
        DialogService dialogService = new DialogService();
        MacroStorageService  macroStorageService = new MacroStorageService();

        // Carrega o FXML injetando o Controller
        FXMLLoader fxmlLoader = new FXMLLoader(AutoClickerApplication.class.getResource("autoClicker-view.fxml"));
        fxmlLoader.setControllerFactory(param -> new AutoClickerController(hotkeyService, macroService, dialogService, macroStorageService));

        Scene scene = new Scene(fxmlLoader.load(), 580, 740);

        // Ícone da aplicação
        try (InputStream iconStream = AutoClickerApplication.class.getResourceAsStream("icon.png")) {
            if (iconStream != null) {
                stage.getIcons().add(new Image(iconStream));
            }
        }

        stage.setTitle("AutoClicker");
        stage.setMinWidth(520);
        stage.setMinHeight(680);
        stage.setScene(scene);

        // Encerramento limpo de threads e hooks ao fechar a janela
        stage.setOnCloseRequest(event -> {
            AutoClickerEngine.stopAutoClicker();
            MacroEngine.stopMacro();
            try {
                GlobalScreen.unregisterNativeHook();
            } catch (NativeHookException ignored) {
            }
            Platform.exit();
            System.exit(0);
        });

        stage.show();
    }
}
