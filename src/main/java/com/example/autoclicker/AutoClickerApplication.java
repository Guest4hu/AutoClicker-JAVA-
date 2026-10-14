package com.example.autoclicker;

import com.example.autoclicker.domain.engine.AutoClickerEngine;
import com.example.autoclicker.domain.engine.MacroEngine;
import com.example.autoclicker.input.hotkey.HotkeyService;
import com.example.autoclicker.input.macro.MacroService;
import com.example.autoclicker.input.window.WindowService;
import com.example.autoclicker.input.window.WindowServiceFactory;
import com.example.autoclicker.storage.MacroStorageService;
import com.example.autoclicker.ui.controller.AutoClickerTabController;
import com.example.autoclicker.ui.controller.MacroTabController;
import com.example.autoclicker.ui.controller.MainController;
import com.example.autoclicker.ui.dialog.DialogService;
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
        // Registra o hook global de teclado e mouse
        GlobalScreen.registerNativeHook();

        // Instanciação de serviços e motores (Injeção de Dependências manual)
        HotkeyService hotkeyService = new HotkeyService();
        MacroService macroService = new MacroService();
        DialogService dialogService = new DialogService();
        MacroStorageService macroStorageService = new MacroStorageService();
        WindowService windowService = WindowServiceFactory.create();

        AutoClickerEngine autoClickerEngine = new AutoClickerEngine();
        MacroEngine macroEngine = new MacroEngine();

        AutoClickerTabController autoClickerTabController = new AutoClickerTabController(
                autoClickerEngine, hotkeyService, dialogService
        );
        MacroTabController macroTabController = new MacroTabController(
                macroEngine, macroService, dialogService, macroStorageService, windowService, hotkeyService
        );
        MainController mainController = new MainController(
                autoClickerTabController, macroTabController, hotkeyService,
                autoClickerEngine, macroEngine, macroService
        );

        // Carrega o FXML principal configurando a ControllerFactory para todos os sub-controladores
        FXMLLoader fxmlLoader = new FXMLLoader(AutoClickerApplication.class.getResource("autoClicker-view.fxml"));
        fxmlLoader.setControllerFactory(clazz -> {
            if (clazz == MainController.class) {
                return mainController;
            } else if (clazz == AutoClickerTabController.class) {
                return autoClickerTabController;
            } else if (clazz == MacroTabController.class) {
                return macroTabController;
            }
            try {
                return clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Falha ao instanciar controller: " + clazz, e);
            }
        });

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
            mainController.stopAll();
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
