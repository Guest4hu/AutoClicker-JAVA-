package com.example.autoclicker.service;

import com.example.autoclicker.DTO.MacroSettingDTO;
import com.example.autoclicker.config.JacksonConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MacroStorageService {
    private final ObjectMapper objectMapper = JacksonConfig.getObjectMapper();
    private static final Path MACRO_DIR = Path.of("storage", "macros");

    public MacroStorageService() {
        try {
            Files.createDirectories(MACRO_DIR);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar pasta para salvar os macros", e);
        }
    }

    public void save(MacroSettingDTO settingDTO) throws IOException {
        String rawName = settingDTO.name().trim().toLowerCase();
        String nomeArquivo = rawName.endsWith(".json") ? rawName : rawName + ".json";
        if (existeTalArquivo(nomeArquivo)) {
            throw new FileSystemException("Já existe arquivo com este nome");
        }
        Path arquivoDestino = MACRO_DIR.resolve(nomeArquivo);
        objectMapper.writeValue(arquivoDestino.toFile(), settingDTO);
    }

    public MacroSettingDTO load(String nomeArquivo) throws IOException {
        String arquivoFinal = nomeArquivo.endsWith(".json") ? nomeArquivo : nomeArquivo + ".json";
        Path caminhoDestino = MACRO_DIR.resolve(arquivoFinal);
        return objectMapper.readValue(caminhoDestino.toFile(), MacroSettingDTO.class);
    }

    public boolean existeTalArquivo(String nomeArquivo) {
        String arquivoFinal = nomeArquivo.endsWith(".json") ? nomeArquivo : nomeArquivo + ".json";
        return Files.exists(MACRO_DIR.resolve(arquivoFinal));
    }

    public List<String> listAllSetings() throws IOException {
        return listAllSettings();
    }

    public List<String> listAllSettings() throws IOException {
        try (var stream = Files.list(MACRO_DIR)) {
            return stream
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString().replace(".json", ""))
                    .sorted()
                    .toList();
        }
    }
}
