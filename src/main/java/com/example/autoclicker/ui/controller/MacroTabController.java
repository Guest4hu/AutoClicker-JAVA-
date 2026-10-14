package com.example.autoclicker.ui.controller;

import com.example.autoclicker.domain.engine.MacroEngine;
import com.example.autoclicker.dto.MacroPlaybackSettingDTO;
import com.example.autoclicker.dto.MacroSettingDTO;
import com.example.autoclicker.dto.WindowInfoDTO;
import com.example.autoclicker.input.hotkey.HotkeyService;
import com.example.autoclicker.input.macro.MacroService;
import com.example.autoclicker.input.window.WindowService;
import com.example.autoclicker.input.window.WindowServiceFactory;
import com.example.autoclicker.storage.MacroStorageService;
import com.example.autoclicker.ui.dialog.DialogService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.function.UnaryOperator;

/**
 * Controlador JavaFX para a aba de gravação, persistência e reprodução de macros.
 * Todas as operações de disco e chamadas FFM ao servidor gráfico rodam assincronamente fora da JavaFX Thread.
 */
public class MacroTabController {

    private final MacroEngine macroEngine;
    private final MacroService macroService;
    private final DialogService dialogService;
    private final MacroStorageService macroStorageService;
    private final WindowService windowService;
    private final HotkeyService hotkeyService;

    private boolean isMacroRecording = false;
    private boolean macroCancelledByUser = false;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    private Button macroButton;

    @FXML
    private Button buttonSaveMacro;

    @FXML
    private ListView<String> macroListView;

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

    @FXML
    private Button runMacroButton;

    @FXML
    private TextArea macroStatusAreas;

    public MacroTabController() {
        this(new MacroEngine(), new MacroService(), new DialogService(), new MacroStorageService(), WindowServiceFactory.create(), new HotkeyService());
    }

    public MacroTabController(MacroEngine macroEngine, MacroService macroService, DialogService dialogService,
                              MacroStorageService macroStorageService, WindowService windowService, HotkeyService hotkeyService) {
        this.macroEngine = macroEngine != null ? macroEngine : new MacroEngine();
        this.macroService = macroService != null ? macroService : new MacroService();
        this.dialogService = dialogService != null ? dialogService : new DialogService();
        this.macroStorageService = macroStorageService != null ? macroStorageService : new MacroStorageService();
        this.windowService = windowService != null ? windowService : WindowServiceFactory.create();
        this.hotkeyService = hotkeyService != null ? hotkeyService : new HotkeyService();
    }

    @FXML
    public void initialize() {
        macroRepeatField.setText("1");
        macroIntervalField.setText("0");
        setupNumericField(macroRepeatField);
        setupNumericField(macroIntervalField);

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

    @FXML
    public void startMacroRec(ActionEvent actionEvent) {
        if (isMacroRecording) {
            macroLog("Gravação já em andamento! Pressione [ESC] para finalizar.");
            return;
        }

        isMacroRecording = true;
        macroButton.setText("Gravando... (ESC para parar)");
        macroLog("Iniciando gravação de macro... Execute suas ações e pressione [ESC] para encerrar.");

        macroService.captureMacro(() -> Platform.runLater(() -> {
            isMacroRecording = false;
            macroButton.setText("Gravar Macro (ESC para parar)");
            macroLog("Gravação concluída com sucesso! Total de ações gravadas: " + macroService.getMacroActions().size());
        }));
    }

    @FXML
    public void saveMacro(ActionEvent actionEvent) {
        if (macroService.getMacroActions().isEmpty()) {
            macroLog("Nenhuma ação gravada para salvar!");
            return;
        }

        dialogService.showMacroDialog().ifPresent(data -> {
            CompletableFuture.runAsync(() -> {
                try {
                    MacroSettingDTO setting = new MacroSettingDTO(
                            data.name(),
                            data.description(),
                            LocalDateTime.now(),
                            macroService.getMacroActions()
                    );

                    macroStorageService.save(setting);
                    Platform.runLater(() -> {
                        macroLog("Macro '" + data.name() + "' salva com sucesso!");
                        refreshMacroList();
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> macroLog("Erro ao salvar macro: " + e.getMessage()));
                }
            });
        });
    }

    @FXML
    public void refreshMacroList() {
        CompletableFuture.supplyAsync(() -> {
            try {
                return macroStorageService.listAllSettings();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(macros -> Platform.runLater(() -> macroListView.getItems().setAll(macros)))
          .exceptionally(ex -> {
              Platform.runLater(() -> macroLog("Erro ao listar macros: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage())));
              return null;
          });
    }

    private void carregarDetalhesMacro(String macroName) {
        CompletableFuture.supplyAsync(() -> {
            try {
                return macroStorageService.load(macroName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).thenAccept(macro -> Platform.runLater(() -> {
            macroStatusAreas.clear();
            macroLog("--- Macro: " + macro.name() + " ---");
            if (macro.description() != null && !macro.description().isBlank()) {
                macroLog("Descrição: " + macro.description());
            }
            macroLog("Criado em: " + macro.createdAt());
            macroLog("Ações gravadas (" + macro.configs().size() + "):");

            macro.configs().forEach(action -> macroLog(action.toString()));
        })).exceptionally(ex -> {
            Platform.runLater(() -> macroLog("Erro ao carregar macro: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage())));
            return null;
        });
    }

    @FXML
    public void runSelectedMacro(ActionEvent event) {
        if (macroEngine.isMacroRunning()) {
            stopMacroPlayback();
            return;
        }

        String selected = macroListView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        MacroPlaybackSettingDTO playback;
        try {
            playback = criarMacroPlaybackSetting();
        } catch (NumberFormatException e) {
            macroLog("Erro: valores numéricos inválidos nas configurações de repetição.");
            return;
        }

        runMacroButton.setDisable(true);
        CompletableFuture.supplyAsync(() -> {
            try {
                return macroStorageService.load(selected);
            } catch (IOException e) {
                throw new RuntimeException("Falha ao ler arquivo: " + e.getMessage(), e);
            }
        }).thenAccept(macro -> Platform.runLater(() -> {
            runMacroButton.setDisable(false);
            if (macroEngine.isMacroRunning()) {
                return;
            }
            try {
                macroLog("Iniciando reprodução da macro: " + selected + " ("
                        + (playback.infinite() ? "infinito" : playback.repeatCount() + "x")
                        + ", intervalo " + playback.intervalMillis() + "ms)... [ESC para parar]");
                runMacroButton.setText("Parar Macro (ESC)");
                macroCancelledByUser = false;

                macroEngine.setIterationListener(run -> Platform.runLater(() ->
                        macroLog("Execução #" + run + (playback.infinite() ? "" : " de " + playback.repeatCount()))
                ));

                macroEngine.setFinishListener(() -> Platform.runLater(() -> {
                    runMacroButton.setText("Executar Macro Selecionada");
                    if (!macroCancelledByUser) {
                        macroLog("Macro '" + selected + "' finalizada com sucesso!");
                    }
                }));

                // Passa a configuração de playback (resolvendo o bug funcional P-01)
                macroEngine.startMacro(macro, playback);

            } catch (Exception e) {
                macroLog("Erro ao executar macro: " + e.getMessage());
                runMacroButton.setText("Executar Macro Selecionada");
            }
        })).exceptionally(ex -> {
            Platform.runLater(() -> {
                runMacroButton.setDisable(false);
                runMacroButton.setText("Executar Macro Selecionada");
                macroLog("Erro ao carregar macro para execução: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
            });
            return null;
        });
    }

    /**
     * Interrompe a macro em execução. Chamado tanto pelo botão quanto pela tecla ESC global.
     */
    public void stopMacroPlayback() {
        if (!macroEngine.isMacroRunning()) {
            return;
        }
        macroCancelledByUser = true;
        macroEngine.stopMacro();
        runMacroButton.setText("Executar Macro Selecionada");
        macroLog("Reprodução da macro cancelada.");
    }

    /**
     * Tratamento global para tecla ESC: se uma macro estiver rodando, interrompe; se estiver gravando, para a gravação.
     */
    public void handleGlobalEscape() {
        if (macroEngine.isMacroRunning()) {
            stopMacroPlayback();
        } else if (isMacroRecording) {
            macroService.stopCapture();
            isMacroRecording = false;
            macroButton.setText("Gravar Macro (ESC para parar)");
            macroLog("Gravação cancelada via ESC.");
        }
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
        CompletableFuture.supplyAsync(windowService::getOpenWindows)
                .thenAccept(windows -> Platform.runLater(() -> {
                    targetWindowCombo.getItems().setAll(windows);
                    if (!windows.isEmpty()) {
                        targetWindowCombo.getSelectionModel().selectFirst();
                        macroLog(windows.size() + " janela(s) encontrada(s).");
                    } else {
                        macroLog("Nenhuma janela encontrada.");
                    }
                })).exceptionally(ex -> {
                    Platform.runLater(() -> macroLog("Erro ao listar janelas: " + (ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage())));
                    return null;
                });
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

    private void macroLog(String message) {
        String timestamp = LocalTime.now().format(TIME_FORMATTER);
        String entry = "[" + timestamp + "] " + message + "\n";
        macroStatusAreas.appendText(entry);
    }

    public MacroEngine getMacroEngine() {
        return macroEngine;
    }

    public MacroService getMacroService() {
        return macroService;
    }
}
