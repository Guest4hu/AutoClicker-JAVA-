package com.example.autoclicker;

import com.example.autoclicker.domain.engine.AutoClickerEngine;
import com.example.autoclicker.domain.engine.MacroEngine;
import com.example.autoclicker.input.hotkey.HotkeyService;
import com.example.autoclicker.input.macro.MacroService;
import com.example.autoclicker.input.simulator.VirtualInputSimulator;
import com.example.autoclicker.input.window.WindowServiceFactory;
import com.example.autoclicker.storage.MacroStorageService;
import com.example.autoclicker.ui.controller.AutoClickerTabController;
import com.example.autoclicker.ui.controller.MacroTabController;
import com.example.autoclicker.ui.controller.MainController;
import com.example.autoclicker.ui.dialog.DialogService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class FXMLValidationTest {

    @Test
    void testFxmlIsWellFormed() throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();

        try (InputStream in = getClass().getResourceAsStream("/com/example/autoclicker/autoClicker-view.fxml")) {
            assertNotNull(in, "autoClicker-view.fxml não encontrado nos resources");
            Document doc = builder.parse(in);
            assertNotNull(doc);
            assertEquals("StackPane", doc.getDocumentElement().getLocalName());
        }

        try (InputStream in = getClass().getResourceAsStream("/com/example/autoclicker/auto-clicker-tab.fxml")) {
            assertNotNull(in, "auto-clicker-tab.fxml não encontrado nos resources");
            Document doc = builder.parse(in);
            assertNotNull(doc);
            assertEquals("ScrollPane", doc.getDocumentElement().getLocalName());
        }

        try (InputStream in = getClass().getResourceAsStream("/com/example/autoclicker/macro-tab.fxml")) {
            assertNotNull(in, "macro-tab.fxml não encontrado nos resources");
            Document doc = builder.parse(in);
            assertNotNull(doc);
            assertEquals("ScrollPane", doc.getDocumentElement().getLocalName());
        }
    }

    @Test
    void testResourcesExist() {
        assertNotNull(getClass().getResourceAsStream("/com/example/autoclicker/styles.css"), "styles.css deve existir");
        assertNotNull(getClass().getResourceAsStream("/com/example/autoclicker/icon.png"), "icon.png deve existir");
        assertNotNull(getClass().getResourceAsStream("/com/example/autoclicker/auto-clicker-tab.fxml"), "auto-clicker-tab.fxml deve existir");
        assertNotNull(getClass().getResourceAsStream("/com/example/autoclicker/macro-tab.fxml"), "macro-tab.fxml deve existir");
    }

    @Test
    void testFXMLLoaderInstantiates() throws Exception {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Toolkit já iniciado
        }

        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Throwable> error = new AtomicReference<>();

        Platform.runLater(() -> {
            try {
                HotkeyService hotkeyService = new HotkeyService();
                AutoClickerEngine autoClickerEngine = new AutoClickerEngine(new VirtualInputSimulator());
                MacroEngine macroEngine = new MacroEngine(new VirtualInputSimulator());
                MacroService macroService = new MacroService();
                DialogService dialogService = new DialogService();
                MacroStorageService storageService = new MacroStorageService();

                AutoClickerTabController tab1 = new AutoClickerTabController(autoClickerEngine, hotkeyService, dialogService);
                MacroTabController tab2 = new MacroTabController(macroEngine, macroService, dialogService, storageService, WindowServiceFactory.create(), hotkeyService);
                MainController main = new MainController(tab1, tab2, hotkeyService, autoClickerEngine, macroEngine, macroService);

                FXMLLoader loader = new FXMLLoader(AutoClickerApplication.class.getResource("autoClicker-view.fxml"));
                loader.setControllerFactory(param -> {
                    if (param == MainController.class) {
                        return main;
                    } else if (param == AutoClickerTabController.class) {
                        return tab1;
                    } else if (param == MacroTabController.class) {
                        return tab2;
                    }
                    try {
                        return param.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

                Object root = loader.load();
                assertNotNull(root);
                assertNotNull(main.getAutoClickerTabController());
                assertNotNull(main.getMacroTabController());
            } catch (Throwable t) {
                error.set(t);
            } finally {
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS), "Timeout ao carregar tela JavaFX");
        if (error.get() != null) {
            throw new AssertionError("Erro ao carregar FXML e Controller no JavaFX Toolkit", error.get());
        }
    }
}
