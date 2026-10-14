package com.example.autoclicker.ui.controller;

import com.example.autoclicker.domain.engine.AutoClickerEngine;
import com.example.autoclicker.domain.model.AutoClickerSetting;
import com.example.autoclicker.domain.model.ClickType;
import com.example.autoclicker.domain.model.MouseButton;
import com.example.autoclicker.domain.model.StatusState;
import com.example.autoclicker.input.hotkey.HotkeyService;
import com.example.autoclicker.ui.dialog.DialogService;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Controlador JavaFX para a aba do AutoClicker.
 * Responsável por gerenciar configurações de clique, validação, animações de CPS e ciclo do motor.
 */
public class AutoClickerTabController {

    private final AutoClickerEngine autoClickerEngine;
    private final HotkeyService hotkeyService;
    private final DialogService dialogService;

    private boolean isRunning = false;
    private long startTimeMillis = 0;
    private Timeline statsTimeline;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    private TextField intervalField;

    @FXML
    private TextField delayField;

    @FXML
    private TextField clickCountField;

    @FXML
    private CheckBox infiniteClicksCheck;

    @FXML
    private ComboBox<ClickType> clickTypeCombo;

    @FXML
    private ComboBox<MouseButton> mouseButtonCombo;

    @FXML
    private Label cpsEstimateLabel;

    @FXML
    private HBox statusBadge;

    @FXML
    private Circle statusDot;

    @FXML
    private Label statusText;

    @FXML
    private Label hotKeyBadge;

    @FXML
    private Button hotKeyButton;

    @FXML
    private Button resetHotkeyBtn;

    @FXML
    private Button startButton;

    @FXML
    private Label statClicksLabel;

    @FXML
    private Label statTimeLabel;

    @FXML
    private Label statCpsLabel;

    @FXML
    private TextArea statusArea;

    @FXML
    private Button clearLogButton;

    public AutoClickerTabController() {
        this(new AutoClickerEngine(), new HotkeyService(), new DialogService());
    }

    public AutoClickerTabController(AutoClickerEngine autoClickerEngine, HotkeyService hotkeyService, DialogService dialogService) {
        this.autoClickerEngine = autoClickerEngine != null ? autoClickerEngine : new AutoClickerEngine();
        this.hotkeyService = hotkeyService != null ? hotkeyService : new HotkeyService();
        this.dialogService = dialogService != null ? dialogService : new DialogService();
    }

    @FXML
    public void initialize() {
        clickTypeCombo.getItems().addAll(ClickType.values());
        mouseButtonCombo.getItems().addAll(MouseButton.values());

        clickTypeCombo.setValue(ClickType.SIMPLES);
        mouseButtonCombo.setValue(MouseButton.ESQUERDO);
        intervalField.setText("100");
        delayField.setText("0");
        clickCountField.setText("100");

        setupNumericField(intervalField);
        setupNumericField(delayField);
        setupNumericField(clickCountField);

        intervalField.textProperty().addListener((obs, oldVal, newVal) -> updateCpsEstimate());
        updateCpsEstimate();

        updateHotkeyDisplay();

        autoClickerEngine.setProgressListener(clicks ->
                Platform.runLater(() -> statClicksLabel.setText(String.format("%,d", clicks)))
        );

        autoClickerEngine.setFinishListener(() ->
                Platform.runLater(this::handleAutoFinished)
        );

        setupStatsTimeline();

        log("AutoClicker pronto. Atalho: " + hotkeyService.getHotkeyText());
    }

    private void setupNumericField(TextField field) {
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (newText.matches("\\d*")) {
                return change;
            }
            return null;
        };
        field.setTextFormatter(new TextFormatter<>(filter));
    }

    private void updateCpsEstimate() {
        try {
            String text = intervalField.getText() != null ? intervalField.getText().trim() : "";
            if (text.isEmpty()) {
                cpsEstimateLabel.setText("- CPS");
                return;
            }
            int interval = Integer.parseInt(text);
            if (interval <= 0) {
                cpsEstimateLabel.setText("Inválido");
            } else if (interval == 1) {
                cpsEstimateLabel.setText("~1000 CPS (Máx)");
            } else {
                double cps = 1000.0 / interval;
                cpsEstimateLabel.setText(String.format("~%.1f CPS", cps));
            }
        } catch (NumberFormatException e) {
            cpsEstimateLabel.setText("Inválido");
        }
    }

    private void setupStatsTimeline() {
        statsTimeline = new Timeline(new KeyFrame(Duration.millis(200), event -> {
            if (!isRunning) return;
            long elapsedMillis = System.currentTimeMillis() - startTimeMillis;
            long seconds = (elapsedMillis / 1000) % 60;
            long minutes = (elapsedMillis / (1000 * 60)) % 60;
            long hours = (elapsedMillis / (1000 * 60 * 60));

            statTimeLabel.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

            int totalClicks = autoClickerEngine.getTotalClicks();
            if (elapsedMillis > 500) {
                double measuredCps = (totalClicks * 1000.0) / elapsedMillis;
                statCpsLabel.setText(String.format("%.1f", measuredCps));
            }
        }));
        statsTimeline.setCycleCount(Animation.INDEFINITE);
    }

    @FXML
    public void handlePreset(ActionEvent event) {
        if (event.getSource() instanceof Button button) {
            String text = button.getText();
            if (text.startsWith("1 ms")) {
                intervalField.setText("1");
            } else if (text.startsWith("10 ms")) {
                intervalField.setText("10");
            } else if (text.startsWith("50 ms")) {
                intervalField.setText("50");
            } else if (text.startsWith("100 ms")) {
                intervalField.setText("100");
            } else if (text.startsWith("500 ms")) {
                intervalField.setText("500");
            } else if (text.startsWith("1000 ms")) {
                intervalField.setText("1000");
            }
        }
    }

    @FXML
    public void disableInfiniteClick() {
        boolean isInfinite = infiniteClicksCheck.isSelected();
        clickCountField.setDisable(isInfinite);
        if (isInfinite) {
            clickCountField.setPromptText("Infinito");
        } else {
            clickCountField.setPromptText("100");
            if (clickCountField.getText() == null || clickCountField.getText().isEmpty()) {
                clickCountField.setText("100");
            }
        }
    }

    @FXML
    public void buttonStopAndStart() {
        if (!isRunning) {
            handleStart();
        } else {
            handleStop();
        }
    }

    /**
     * Alterna o estado de execução (iniciar/parar), invocado externamente pelo hotkey global.
     */
    public void toggleAutoClicker() {
        buttonStopAndStart();
    }

    public void handleStart() {
        try {
            AutoClickerSetting setting = criarAutoClickerSetting();
            List<String> errors = setting.validateSetting();
            if (!errors.isEmpty()) {
                log("Erro na validação:\n- " + String.join("\n- ", errors));
                return;
            }

            autoClickerEngine.setSetting(setting);
            autoClickerEngine.startAutoClicker();

            isRunning = true;
            startTimeMillis = System.currentTimeMillis();
            statClicksLabel.setText("0");
            statTimeLabel.setText("00:00:00");
            statCpsLabel.setText("0.0");
            statsTimeline.play();

            updateStatusView(StatusState.RUNNING);

            log("Iniciado (" + (setting.isInfiniteClick() ? "Infinito" : setting.getClickCount() + " cliques")
                    + ", " + setting.getInterval() + "ms)");
        } catch (NumberFormatException e) {
            log("Erro: preencha valores inteiros válidos.");
        } catch (IllegalStateException e) {
            log("Erro: " + e.getMessage());
        }
    }

    public void handleStop() {
        autoClickerEngine.stopAutoClicker();
        isRunning = false;
        if (statsTimeline != null) {
            statsTimeline.stop();
        }

        updateStatusView(StatusState.READY);
        log("Parado.");
    }

    private void handleAutoFinished() {
        isRunning = false;
        if (statsTimeline != null) {
            statsTimeline.stop();
        }

        updateStatusView(StatusState.READY);
        int finalClicks = autoClickerEngine.getTotalClicks();
        long elapsedMillis = Math.max(1, System.currentTimeMillis() - startTimeMillis);
        double totalSeconds = elapsedMillis / 1000.0;
        log(String.format("Finalizado: %,d cliques em %.2fs.", finalClicks, totalSeconds));
    }

    private void updateStatusView(StatusState state) {
        statusBadge.getStyleClass().removeAll("status-badge-ready", "status-badge-running", "status-badge-recording");
        startButton.getStyleClass().removeAll("btn-action-start", "btn-action-stop");

        String hotkeyText = hotkeyService.getHotkeyText();

        switch (state) {
            case RUNNING -> {
                statusBadge.getStyleClass().add("status-badge-running");
                statusDot.setFill(Color.web("#ef4444"));
                statusText.setText("ATIVO");

                startButton.setText("Parar (" + hotkeyText + ")");
                startButton.getStyleClass().add("btn-action-stop");
            }
            case RECORDING -> {
                statusBadge.getStyleClass().add("status-badge-recording");
                statusDot.setFill(Color.web("#f97316"));
                statusText.setText("GRAVANDO");

                hotKeyBadge.getStyleClass().add("key-badge-recording");
                hotKeyBadge.setText("Aperte tecla...");
                hotKeyButton.setDisable(true);
                resetHotkeyBtn.setDisable(true);
            }
            case READY -> {
                statusBadge.getStyleClass().add("status-badge-ready");
                statusDot.setFill(Color.web("#a3a3a3"));
                statusText.setText("PRONTO");

                hotKeyBadge.getStyleClass().remove("key-badge-recording");
                updateHotkeyDisplay();

                startButton.setText("Iniciar (" + hotkeyText + ")");
                startButton.getStyleClass().add("btn-action-start");

                hotKeyButton.setDisable(false);
                resetHotkeyBtn.setDisable(false);
            }
        }
    }

    public AutoClickerSetting criarAutoClickerSetting() {
        String intervalText = intervalField.getText() != null ? intervalField.getText().trim() : "";
        int interval = intervalText.isEmpty() ? 100 : Integer.parseInt(intervalText);

        String delayText = delayField.getText() != null ? delayField.getText().trim() : "";
        int delay = delayText.isEmpty() ? 0 : Integer.parseInt(delayText);

        boolean infinite = infiniteClicksCheck.isSelected();
        int clickCount = 0;
        if (!infinite) {
            String countText = clickCountField.getText() != null ? clickCountField.getText().trim() : "";
            clickCount = countText.isEmpty() ? 100 : Integer.parseInt(countText);
        }

        ClickType clickType = clickTypeCombo.getValue();
        MouseButton mouseButton = mouseButtonCombo.getValue();

        if (infinite) {
            return AutoClickerSetting.infiniteClicks(interval, clickType, mouseButton, delay);
        } else {
            return AutoClickerSetting.limitedClicks(interval, clickType, mouseButton, delay, clickCount);
        }
    }

    @FXML
    public void selectHotKeyButton(ActionEvent actionEvent) {
        if (isRunning) {
            log("Pare a execução antes de alterar a tecla.");
            return;
        }

        updateStatusView(StatusState.RECORDING);
        log("Pressione a nova tecla (ESC cancela)...");

        hotkeyService.captureHotkey(() -> {
            Platform.runLater(() -> {
                updateStatusView(StatusState.READY);
                log("Atalho alterado para: " + hotkeyService.getHotkeyText());
            });
        });
    }

    @FXML
    public void resetDefaultHotkey(ActionEvent actionEvent) {
        if (isRunning) {
            log("Pare a execução antes de alterar a tecla.");
            return;
        }
        hotkeyService.resetDefaultHotkey();
        updateHotkeyDisplay();
        startButton.setText("Iniciar (" + hotkeyService.getHotkeyText() + ")");
        log("Atalho redefinido: " + hotkeyService.getHotkeyText());
    }

    private void updateHotkeyDisplay() {
        String key = hotkeyService.getHotkeyText();
        hotKeyBadge.setText(key);
        if (!isRunning) {
            startButton.setText("Iniciar (" + key + ")");
        }
    }

    @FXML
    public void clearLog() {
        statusArea.clear();
    }

    private void log(String message) {
        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        String entry = "[" + timestamp + "] " + message + "\n";
        statusArea.appendText(entry);
    }

    public AutoClickerEngine getAutoClickerEngine() {
        return autoClickerEngine;
    }
}
