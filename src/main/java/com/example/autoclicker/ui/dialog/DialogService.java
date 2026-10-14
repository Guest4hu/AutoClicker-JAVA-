package com.example.autoclicker.ui.dialog;

import com.example.autoclicker.dto.MacroDialogDTO;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

import java.util.Optional;

/**
 * Serviço responsável por exibir caixas de diálogo modais JavaFX na interface.
 */
public class DialogService {

    /**
     * Exibe o diálogo modal para solicitação do nome e descrição da macro a salvar.
     */
    public Optional<MacroDialogDTO> showMacroDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Salvar Macro");
        dialog.setHeaderText("Defina as informações da sua macro");
        ButtonType salvarBtnType = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(salvarBtnType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 20, 10, 10));

        TextField nameField = new TextField();
        nameField.setPromptText("Nome do arquivo (ex: farm_ouro)");

        TextField descField = new TextField();
        descField.setPromptText("Descrição da macro (opcional)");

        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Descrição:"), 0, 1);
        grid.add(descField, 1, 1);

        dialog.getDialogPane().setContent(grid);
        Platform.runLater(nameField::requestFocus);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == salvarBtnType) {
            String nome = nameField.getText().trim();
            String desc = descField.getText().trim();
            if (!nome.isEmpty()) {
                return Optional.of(new MacroDialogDTO(nome, desc));
            }
        }
        return Optional.empty();
    }
}
