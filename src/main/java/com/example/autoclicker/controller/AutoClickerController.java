package com.example.autoclicker.controller;

import com.example.autoclicker.DTO.MacroSettingDTO;
import com.example.autoclicker.DTO.WindowInfoDTO;
import com.example.autoclicker.config.OperationalSystemConfig;
import com.example.autoclicker.domain.AutoClickerEngine;
import com.example.autoclicker.domain.AutoClickerSetting;
import com.example.autoclicker.domain.MacroEngine;
import com.example.autoclicker.DTO.MacroPlaybackSettingDTO;
import com.example.autoclicker.enums.ClickType;
import com.example.autoclicker.enums.MouseButton;
import com.example.autoclicker.enums.StatusState;
import com.example.autoclicker.service.DialogService;
import com.example.autoclicker.service.HotkeyService;
import com.example.autoclicker.service.MacroService;
import com.example.autoclicker.service.MacroStorageService;
import com.example.autoclicker.service.WindowService;
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

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.UnaryOperator;

public class AutoClickerController {

    private final HotkeyService hotkeyService;
    private final MacroService macroService;
    private final DialogService dialogService;
    private final MacroStorageService macroStorageService;
    private final WindowService windowService;
    private final AutoClickerEngine autoClickerEngine = new AutoClickerEngine();
    private final MacroEngine macroEngine = new MacroEngine();

    private boolean isMacroRecording = false;
    private boolean macroCancelledByUser = false;
    private boolean isRunning = false;
    private long startTimeMillis = 0;
    private Timeline statsTimeline;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    private TextField intervalField;

    @FXML
    private ListView<String> macroListView;

    @FXML
    private Button runMacroButton;

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
    private Button macroButton;

    @FXML
    private Button buttonSaveMacro;

    @FXML
    private TextArea macroStatusAreas;

    @FXML
    private CheckBox macroInfiniteCheck;

    @FXML
    private TextField macroRepeatField;

    @FXML
    private TextField macroIntervalField;

    @FXML
    private CheckBox targetWindowCheck;

    @FXML
    private ComboBox<WindowInfoDTO> targetWindowCombo;

    @FXML
    private Button refreshWindowsBtn;

    /**
     * Construtor padrão sem argumentos exigido pelo JavaFX e IDEs (SceneBuilder/IntelliJ).
     */
    public AutoClickerController() {
        this(new HotkeyService(), new MacroService(), new DialogService(), new MacroStorageService(), OperationalSystemConfig.create());
    }

    public AutoClickerController(HotkeyService hotkeyService) {
        this(hotkeyService, new MacroService(), new DialogService(), new MacroStorageService(), OperationalSystemConfig.create());
    }

    /**
     * Construtor com injeção de dependências utilizado pela Factory na Application.
     */
    public AutoClickerController(HotkeyService hotkeyService, MacroService macroService, DialogService dialogService, MacroStorageService macroStorageService) {
        this(hotkeyService, macroService, dialogService, macroStorageService, OperationalSystemConfig.create());
    }

    public AutoClickerController(HotkeyService hotkeyService, MacroService macroService, DialogService dialogService, MacroStorageService macroStorageService, WindowService windowService) {
        this.hotkeyService = hotkeyService;
        this.macroService = macroService;
        this.dialogService = dialogService;
        this.macroStorageService = macroStorageService;
        this.windowService = windowService;
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

        macroRepeatField.setText("1");
        macroIntervalField.setText("0");
        setupNumericField(macroRepeatField);
        setupNumericField(macroIntervalField);

        intervalField.textProperty().addListener((obs, oldVal, newVal) -> updateCpsEstimate());
        updateCpsEstimate();

        updateHotkeyDisplay();

        AutoClickerEngine.setProgressListener(clicks -> {
            Platform.runLater(() -> statClicksLabel.setText(String.format("%,d", clicks)));
        });

        autoClickerEngine.setFinishListener(() -> {
            Platform.runLater(this::handleAutoFinished);
        });

        setupStatsTimeline();

        log("AutoClicker iniciado. Atalho: " + hotkeyService.getHotkeyText());

        Platform.runLater(this::ativacaoTeclaDeAtalho);

        macroListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selectedMacro) -> {
            if (selectedMacro != null) {
                runMacroButton.setDisable(false);
                carregarDetalhesMacro(selectedMacro);
            } else {
                runMacroButton.setDisable(true);
            }
        });

        refreshMacroList();
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

            int totalClicks = AutoClickerEngine.getTotalClicks();
            if (elapsedMillis > 500) {
                double measuredCps = (totalClicks * 1000.0) / elapsedMillis;
                statCpsLabel.setText(String.format("%.1f", measuredCps));
            }
        }));
        statsTimeline.setCycleCount(Animation.INDEFINITE);
    }

    public void ativacaoTeclaDeAtalho() {
        hotkeyService.start(() -> {
            Platform.runLater(this::buttonStopAndStart);
        });

        // ESC global: interrompe a macro em execução, mesmo com a janela minimizada
        hotkeyService.registerStopKey(() -> Platform.runLater(this::stopMacroPlayback));
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
    public void startMacroRec(ActionEvent actionEvent) {
        if (isMacroRecording) {
            macroLog("Gravação já em andamento! Pressione [ESC] para finalizar.");
            return;
        }

        isMacroRecording = true;
        macroButton.setText("Gravando... (ESC para parar)");
        macroLog("Iniciando gravação de macro... Execute suas ações e pressione [ESC] para encerrar.");

        macroService.captureMacro(() -> {
            Platform.runLater(() -> {
                isMacroRecording = false;
                macroButton.setText("Gravar Macro (ESC para parar)");
                macroLog("Gravação concluída com sucesso! Total de ações gravadas: " + macroService.getMacroActions().size());
            });
        });
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

    public void handleStart() {
        try {
            AutoClickerSetting autoClickerSetting = criarAutoClickerSetting();
            List<String> errors = autoClickerSetting.validateSetting();
            if (!errors.isEmpty()) {
                log("Erro na validação:\n- " + String.join("\n- ", errors));
                return;
            }

            AutoClickerEngine.setSetting(autoClickerSetting);
            AutoClickerEngine.startAutoClicker();

            isRunning = true;
            startTimeMillis = System.currentTimeMillis();
            statClicksLabel.setText("0");
            statTimeLabel.setText("00:00:00");
            statCpsLabel.setText("0.0");
            statsTimeline.play();

            updateStatusView(StatusState.RUNNING);

            log("Iniciado (" + (autoClickerSetting.isInfiniteClick() ? "Infinito" : autoClickerSetting.getClickCount() + " cliques")
                    + ", " + autoClickerSetting.getInterval() + "ms)");
        } catch (NumberFormatException e) {
            log("Erro: preencha valores inteiros válidos.");
        } catch (IllegalStateException e) {
            log("Erro: " + e.getMessage());
        }
    }

    public void handleStop() {
        AutoClickerEngine.stopAutoClicker();
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
        int finalClicks = AutoClickerEngine.getTotalClicks();
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

    private void macroLog(String message) {
        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        String entry = "[" + timestamp + "] " + message + "\n";
        macroStatusAreas.appendText(entry);
    }

    @FXML
    public void saveMacro(ActionEvent actionEvent) {
        if (macroService.getMacroActions().isEmpty()) {
            macroLog("Nenhuma ação gravada para salvar!");
            return;
        }

        dialogService.showMacroDialog().ifPresent(data -> {
            try {
                MacroSettingDTO setting = new MacroSettingDTO(
                        data.name(),
                        data.description(),
                        LocalDateTime.now(),
                        macroService.getMacroActions()
                );

                macroStorageService.save(setting);
                macroLog("Macro '" + data.name() + "' salva com sucesso!");

            } catch (Exception e) {
                macroLog("Erro ao salvar macro: " + e.getMessage());
            }
        });
        refreshMacroList();
    }

    @FXML
    public void refreshMacroList() {
        try {
            List<String> macros = macroStorageService.listAllSettings();
            macroListView.getItems().setAll(macros);
        } catch (IOException e) {
            macroLog("Erro ao listar macros: " + e.getMessage());
        }
    }

    private void carregarDetalhesMacro(String macroName) {
        try {
            MacroSettingDTO macro = macroStorageService.load(macroName);
            macroStatusAreas.clear();
            macroLog("--- Macro: " + macro.name() + " ---");
            if (macro.description() != null && !macro.description().isBlank()) {
                macroLog("Descrição: " + macro.description());
            }
            macroLog("Criado em: " + macro.createdAt());
            macroLog("Ações gravadas (" + macro.configs().size() + "):");

            macro.configs().forEach(action -> macroLog(action.toString()));
        } catch (IOException e) {
            macroLog("Erro ao carregar macro: " + e.getMessage());
        }
    }

    @FXML
    public void runSelectedMacro(ActionEvent event) {
        if (MacroEngine.isMacroRunning()) {
            stopMacroPlayback();
            return;
        }

        String selected = macroListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        try {
            MacroPlaybackSettingDTO playback = criarMacroPlaybackSetting();
            MacroSettingDTO macro = macroStorageService.load(selected);
            macroLog("Iniciando reprodução da macro: " + selected + " ("
                    + (playback.infinite() ? "infinito" : playback.repeatCount() + "x")
                    + ", intervalo " + playback.intervalMillis() + "ms)... [ESC para parar]");
            runMacroButton.setText("Parar Macro (ESC)");
            macroCancelledByUser = false;

            macroEngine.setIterationListener(run -> Platform.runLater(() ->
                    macroLog("Execução #" + run + (playback.infinite() ? "" : " de " + playback.repeatCount()))
            ));

            macroEngine.setFinishListener(() -> {
                Platform.runLater(() -> {
                    runMacroButton.setText("Executar Macro Selecionada");
                    if (!macroCancelledByUser) {
                        macroLog("Macro '" + selected + "' finalizada com sucesso!");
                    }
                });
            });
            MacroEngine.startMacro(macro);

        } catch (Exception e) {
            macroLog("Erro ao executar macro: " + e.getMessage());
            runMacroButton.setText("Executar Macro Selecionada");
        }
    }

    /**
     * Interrompe a macro em execução. Chamado tanto pelo botão quanto pela tecla ESC global.
     * Deve rodar na thread do JavaFX, pois atualiza componentes da interface.
     */
    private void stopMacroPlayback() {
        if (!MacroEngine.isMacroRunning()) {
            return;
        }
        macroCancelledByUser = true;
        MacroEngine.stopMacro();
        runMacroButton.setText("Executar Macro Selecionada");
        macroLog("Reprodução da macro cancelada.");
    }

    @FXML
    public void disableMacroInfinite() {
        boolean isInfinite = macroInfiniteCheck.isSelected();
        macroRepeatField.setDisable(isInfinite);
        if (!isInfinite && (macroRepeatField.getText() == null || macroRepeatField.getText().isBlank())) {
            macroRepeatField.setText("1");
        }
    }

    @FXML
    public void toggleTargetWindowMode() {
        boolean enabled = targetWindowCheck.isSelected();
        targetWindowCombo.setDisable(!enabled);
        refreshWindowsBtn.setDisable(!enabled);
        if (enabled && targetWindowCombo.getItems().isEmpty()) {
            refreshOpenWindows();
        }
    }

    @FXML
    public void refreshOpenWindows() {
        macroLog("Buscando janelas abertas...");
        List<WindowInfoDTO> windows = windowService.getOpenWindows();
        targetWindowCombo.getItems().setAll(windows);
        if (!windows.isEmpty()) {
            targetWindowCombo.getSelectionModel().selectFirst();
            macroLog(windows.size() + " janela(s) encontrada(s).");
        } else {
            macroLog("Nenhuma janela encontrada.");
        }
    }

    private MacroPlaybackSettingDTO criarMacroPlaybackSetting() {
        String intervalText = macroIntervalField.getText() != null ? macroIntervalField.getText().trim() : "";
        long interval = intervalText.isEmpty() ? 0 : Long.parseLong(intervalText);

        if (macroInfiniteCheck.isSelected()) {
            return MacroPlaybackSettingDTO.infiniteRuns(interval);
        }

        String repeatText = macroRepeatField.getText() != null ? macroRepeatField.getText().trim() : "";
        int repeatCount = repeatText.isEmpty() ? 1 : Integer.parseInt(repeatText);
        return MacroPlaybackSettingDTO.limitedRuns(repeatCount, interval);
    }
}
