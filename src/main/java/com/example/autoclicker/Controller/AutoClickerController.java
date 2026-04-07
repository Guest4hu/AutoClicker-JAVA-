package com.example.autoclicker.Controller;

import com.example.autoclicker.domain.AutoClickerEngine;
import com.example.autoclicker.domain.AutoClickerSetting;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class AutoClickerController {
	private Set<KeyCode> teclasPressionadas = new LinkedHashSet<>();
	private boolean applicationInitialize = true;
	private static Set<KeyCode> configuredHotKey = new LinkedHashSet<>();
	private Set<KeyCode> currentPressedKeys = new LinkedHashSet<>();
	private boolean capturandoHotkey = false;
	private boolean hotkeyJaDisparou = false;
	private com.example.autoclicker.service.GlobalHotkeyListener globalHotkeyListener;


	@FXML
	private TextField intervalField;

	@FXML
	private TextField delayField;

	@FXML
	private TextField clickCountField;

	@FXML
	private CheckBox infiniteClicksCheck;

	@FXML
	private ComboBox<AutoClickerSetting.ClickType> clickTypeCombo;

	@FXML
	private ComboBox<AutoClickerSetting.MouseButton> mouseButtonCombo;

	@FXML
	private Button hotKeyButton;

	@FXML
	private Button startButton;

	@FXML
	private TextArea statusArea;

//	@FXML
//	private ListView<String> pointsList;
//
//	@FXML
//	private Button addPointButton;
//
//	@FXML
//	private Button removePointButton;
//
//	@FXML
//	private Button captureButton;
//
//	@FXML
//	private Button clearButton;

	@FXML
	public void initialize() {
		// Preenchendo o ComboBox com enum
			clickTypeCombo.getItems().addAll(AutoClickerSetting.ClickType.values());
			mouseButtonCombo.getItems().addAll(AutoClickerSetting.MouseButton.values());
			//Definindo os valores padrões dos ComboBox
			clickTypeCombo.setValue(AutoClickerSetting.ClickType.SIMPLES);
			mouseButtonCombo.setValue(AutoClickerSetting.MouseButton.ESQUERDO);
			intervalField.setText("1000");
			delayField.setText("0");
			clickCountField.setText("100");

			javafx.application.Platform.runLater(() -> {ativacaoTeclaDeAtalho();});


	}

	public void ativacaoTeclaDeAtalho() {
		Scene scene = hotKeyButton.getScene();
		if (scene == null) {
			return;
		}

		globalHotkeyListener = new com.example.autoclicker.service.GlobalHotkeyListener();
		globalHotkeyListener.start(configuredHotKey, () -> {
			if (!capturandoHotkey && !configuredHotKey.isEmpty()) {
				javafx.application.Platform.runLater(this::buttonStopAndStart);
			}
		});

		// Listener local (quando a janela está em foco)
		scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, e -> {
			if (capturandoHotkey || configuredHotKey.isEmpty()) return;
			currentPressedKeys.add(e.getCode());

			if (!hotkeyJaDisparou && currentPressedKeys.containsAll(configuredHotKey)) {
				hotkeyJaDisparou = true;
				buttonStopAndStart();
			}
		});

		scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_RELEASED, e -> {
			currentPressedKeys.remove(e.getCode());
			if (!currentPressedKeys.containsAll(configuredHotKey)) {
				hotkeyJaDisparou = false;
			}
		});
	}



	public void disableInfiniteClick() {
		clickCountField.setDisable(infiniteClicksCheck.isSelected());
		if (infiniteClicksCheck.isSelected()) {
			clickCountField.clear();
		}
	}


	public void buttonStopAndStart( ) {
		if (applicationInitialize) {
			handleStart();
		} else {
			handleStop();
		}
		controlStyleButtonStartAndStop();
	}


	//Trocando o de texto e cor do botão de iniciar/parar para ficar mais intuitivo para o usuário
	public void controlStyleButtonStartAndStop(){
		if (applicationInitialize) {
			startButton.setText("Iniciar");
			startButton.setStyle("-fx-background-color: #00ff00; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10;");
		} else {
			startButton.setText("Parar");
			startButton.setStyle("-fx-background-color: #ff0000; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 10;");
		}
	}

	public void handleStop() {
		AutoClickerEngine.stopAutoClicker();
		applicationInitialize = true;
		statusArea.setText("AutoClicker parado.");
	}



	public void handleStart( ) {
		try {
			AutoClickerSetting autoClickerSetting = criarAutoClickerSetting();
			List<String> errors = autoClickerSetting.validateSetting();
			if (!errors.isEmpty()) {
				statusArea.setText("Erros de validacao:\n- " + String.join("\n- ", errors));
				return;
			}

			AutoClickerEngine.setSetting(autoClickerSetting);
			AutoClickerEngine.startAutoClicker();
			applicationInitialize = false;
			statusArea.setText("AutoClicker iniciado.");
		} catch (NumberFormatException e) {
			statusArea.setText("Preencha intervalo, atraso e quantidade com numeros inteiros validos.");
		} catch (IllegalStateException e) {
			statusArea.setText(e.getMessage());
		}
	}

	public AutoClickerSetting criarAutoClickerSetting() {
		int interval = Integer.parseInt(intervalField.getText());
		int delay = Integer.parseInt(delayField.getText());
		int clickCount = infiniteClicksCheck.isSelected() ? 0 : Integer.parseInt(clickCountField.getText());
		AutoClickerSetting.ClickType clickType = clickTypeCombo.getValue();
		AutoClickerSetting.MouseButton mouseButton = mouseButtonCombo.getValue();


		if (infiniteClicksCheck.isSelected()) {
			return AutoClickerSetting.infiniteClicks(interval, clickType, mouseButton, delay);
		} else {
			return AutoClickerSetting.limitedClicks(interval, clickType, mouseButton, delay, clickCount);
		}
	}


	public void selectHotKeyButton(ActionEvent actionEvent) {
		hotKeyButton.setText("Por favor aperte a tecla... (Para Salvar apenas soltei a 1ª tecla pressionada, para cancelar a captura aperte ESC)");
		hotKeyButton.setDisable(true);
		captureHotKey();
	}

	public void captureHotKey() {
		//Limpar as teclas de atalho antiga
		teclasPressionadas.clear();
		Scene scene = hotKeyButton.getScene();
		scene.getRoot().requestFocus();
		capturandoHotkey = true;

		final KeyCode[] primaryKey = new KeyCode[1];// Salvando a primeira tecla pressionada, para comparar na liberação
		final javafx.event.EventHandler<javafx.scene.input.KeyEvent>[] pressHandler = new javafx.event.EventHandler[1];
		final javafx.event.EventHandler<javafx.scene.input.KeyEvent>[] releaseHandler = new javafx.event.EventHandler[1];

		pressHandler[0] = event -> {
			if (event.getCode() == KeyCode.ESCAPE) {
				statusArea.setText("Captura cancelada.");
				hotKeyButton.setText("Selecionar Tecla de Atalho");
				stopHotKeyCapture(scene, pressHandler[0], releaseHandler[0]);
				return;
			}

			if(event.getCode() == KeyCode.UNDEFINED) {
				statusArea.setText("Tecla não reconhecida. Por favor, tente novamente.");
				return;
			}
			teclasPressionadas.add(event.getCode());
			statusArea.setText("Atalho no momento: " + teclasPressionadas);
			if (primaryKey[0] == null) {
				primaryKey[0] = event.getCode();
			}
		};

		//Caso a primeira tecla pressionada seja soltada este e o momento que ira salvar a combinação de teclas, caso contrário, apenas remove a tecla solta da combinação
		releaseHandler[0] = event -> {
			if (event.getCode() == primaryKey[0]) {
				hotKeyButton.setText("Hotkey: " + teclasPressionadas);
				statusArea.setText("Atalho salvo: " + teclasPressionadas);
				configuredHotKey.clear();
				configuredHotKey.addAll(teclasPressionadas);
				
				// Re-registrar listener global com nova hotkey
				if (globalHotkeyListener != null) {
					globalHotkeyListener.stop();
					globalHotkeyListener.start(configuredHotKey, () -> {
						if (!capturandoHotkey && !configuredHotKey.isEmpty()) {
							javafx.application.Platform.runLater(this::buttonStopAndStart);
						}
					});
				}

				stopHotKeyCapture(scene, pressHandler[0], releaseHandler[0]);
			} else {
				teclasPressionadas.remove(event.getCode());
			}
		};

		scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, pressHandler[0]);
		scene.addEventFilter(javafx.scene.input.KeyEvent.KEY_RELEASED, releaseHandler[0]);
	}

	private void stopHotKeyCapture(Scene scene, javafx.event.EventHandler<javafx.scene.input.KeyEvent> pressHandler, javafx.event.EventHandler<javafx.scene.input.KeyEvent> releaseHandler) {
		scene.removeEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, pressHandler);
		scene.removeEventFilter(javafx.scene.input.KeyEvent.KEY_RELEASED, releaseHandler);
		hotKeyButton.setDisable(false);
		capturandoHotkey = false;
	}



}
