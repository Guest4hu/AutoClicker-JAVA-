package com.example.autoclicker.ui.controller;

import com.example.autoclicker.domain.engine.AutoClickerEngine;
import com.example.autoclicker.domain.engine.MacroEngine;
import com.example.autoclicker.input.hotkey.HotkeyService;
import com.example.autoclicker.input.macro.MacroService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;

/**
 * Controlador principal da aplicação, coordenando a visão composta (Composite View)
 * e intermediando os atalhos globais de teclado e o encerramento limpo dos motores.
 */
public class MainController {

    private final HotkeyService hotkeyService;
    private final AutoClickerEngine autoClickerEngine;
    private final MacroEngine macroEngine;
    private final MacroService macroService;

    @FXML
    private ScrollPane autoClickerTab;

    @FXML
    private AutoClickerTabController autoClickerTabController;

    @FXML
    private ScrollPane macroTab;

    @FXML
    private MacroTabController macroTabController;

    public MainController() {
        this(new HotkeyService(), new AutoClickerEngine(), new MacroEngine(), new MacroService());
    }

    public MainController(HotkeyService hotkeyService) {
        this(hotkeyService, new AutoClickerEngine(), new MacroEngine(), new MacroService());
    }

    public MainController(HotkeyService hotkeyService, AutoClickerEngine autoClickerEngine,
                          MacroEngine macroEngine, MacroService macroService) {
        this.hotkeyService = hotkeyService != null ? hotkeyService : new HotkeyService();
        this.autoClickerEngine = autoClickerEngine;
        this.macroEngine = macroEngine;
        this.macroService = macroService;
    }

    public MainController(AutoClickerTabController autoClickerTabController, MacroTabController macroTabController,
                          HotkeyService hotkeyService, AutoClickerEngine autoClickerEngine,
                          MacroEngine macroEngine, MacroService macroService) {
        this.autoClickerTabController = autoClickerTabController;
        this.macroTabController = macroTabController;
        this.hotkeyService = hotkeyService != null ? hotkeyService : new HotkeyService();
        this.autoClickerEngine = autoClickerEngine;
        this.macroEngine = macroEngine;
        this.macroService = macroService;
    }

    @FXML
    public void initialize() {
        // Vincula a ativação do atalho global do AutoClicker
        hotkeyService.start(() -> Platform.runLater(() -> {
            if (autoClickerTabController != null) {
                autoClickerTabController.toggleAutoClicker();
            }
        }));

        // Vincula a tecla global de escape para interrupção de macros em execução ou gravação
        hotkeyService.registerStopKey(() -> Platform.runLater(() -> {
            if (macroTabController != null) {
                macroTabController.handleGlobalEscape();
            }
        }));
    }

    /**
     * Encerramento e interrupção imediata de todos os motores e serviços em execução.
     */
    public void stopAll() {
        if (autoClickerEngine != null) {
            autoClickerEngine.stop();
        }
        if (macroEngine != null) {
            macroEngine.stop();
        }
        if (macroService != null) {
            macroService.stopCapture();
        }
    }

    public AutoClickerTabController getAutoClickerTabController() {
        return autoClickerTabController;
    }

    public MacroTabController getMacroTabController() {
        return macroTabController;
    }

    public HotkeyService getHotkeyService() {
        return hotkeyService;
    }

    public AutoClickerEngine getAutoClickerEngine() {
        return autoClickerEngine;
    }

    public MacroEngine getMacroEngine() {
        return macroEngine;
    }

    public MacroService getMacroService() {
        return macroService;
    }
}
