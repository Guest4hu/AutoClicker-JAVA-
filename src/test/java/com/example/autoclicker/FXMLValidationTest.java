package com.example.autoclicker;

import com.example.autoclicker.controller.AutoClickerController;
import com.example.autoclicker.service.HotkeyService;
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
    }

    @Test
    void testResourcesExist() {
        assertNotNull(getClass().getResourceAsStream("/com/example/autoclicker/styles.css"), "styles.css deve existir");
        assertNotNull(getClass().getResourceAsStream("/com/example/autoclicker/icon.png"), "icon.png deve existir");
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
                FXMLLoader loader = new FXMLLoader(AutoClickerApplication.class.getResource("autoClicker-view.fxml"));
                loader.setControllerFactory(param -> new AutoClickerController(hotkeyService));
                Object root = loader.load();
                assertNotNull(root);
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
