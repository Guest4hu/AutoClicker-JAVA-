package com.example.autoclicker.controller;

import com.example.autoclicker.domain.AutoClickerEngine;
import com.example.autoclicker.domain.AutoClickerSetting;
import com.example.autoclicker.service.HotkeyService;
import javafx.application.Platform;
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
import java.util.function.Consumer;


public class AutoClickerController {



	private final HotkeyService hotkeyService;

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

    public AutoClickerController(HotkeyService hotkeyService) {
        this.hotkeyService = hotkeyService;
    }

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
		hotkeyService.start(() -> {
			Platform.runLater(this::buttonStopAndStart);
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
		hotkeyService.captureHotkey(() -> {
			// Como estamos voltando da thread do JNativeHook para a tela:
			Platform.runLater(() -> {
				// 1. Atualizamos o texto do botão com o atalho formatado
				hotKeyButton.setText("Atalho: " + hotkeyService.getHotkeyText());

				// 2. Destravamos o botão para poder ser clicado novamente
				hotKeyButton.setDisable(false);
			});
		});
	}
}
