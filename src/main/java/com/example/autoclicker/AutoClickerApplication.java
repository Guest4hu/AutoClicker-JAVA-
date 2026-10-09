package com.example.autoclicker;

import com.example.autoclicker.controller.AutoClickerController;
import com.example.autoclicker.service.HotkeyService;
import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class AutoClickerApplication extends Application {
    @Override
    public void start(Stage stage) throws IOException, NativeHookException {
        GlobalScreen.registerNativeHook();

        // 1. Criamos a instância do serviço
        HotkeyService hotkeyService = new HotkeyService();

        // 2. Carregamos o FXML ensinando a criar o Controller com o serviço
        FXMLLoader fxmlLoader = new FXMLLoader(AutoClickerApplication.class.
                getResource("autoClicker-view.fxml"));
        fxmlLoader.setControllerFactory(param -> new
                AutoClickerController(hotkeyService));

        Scene scene = new Scene(fxmlLoader.load(), 1000, 800);
        stage.setTitle("AutoClicker");
        stage.setScene(scene);
        stage.show();
    }
}
