package com.example.autoclicker.storage;

import com.example.autoclicker.dto.MacroSettingDTO;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Serviço de persistência e recuperação de macros em arquivos JSON no disco local.
 * Suporta injeção de diretório base e ObjectMapper para isolamento completo em testes.
 */
public class MacroStorageService {

    private static final Path DEFAULT_MACRO_DIR = Path.of("storage", "macros");

    private final ObjectMapper objectMapper;
    private final Path macroDir;

    public MacroStorageService() {
        this(DEFAULT_MACRO_DIR, JacksonConfig.getObjectMapper());
    }

    public MacroStorageService(Path macroDir) {
        this(macroDir, JacksonConfig.getObjectMapper());
    }

    public MacroStorageService(Path macroDir, ObjectMapper objectMapper) {
        this.macroDir = macroDir != null ? macroDir : DEFAULT_MACRO_DIR;
        this.objectMapper = objectMapper != null ? objectMapper : JacksonConfig.getObjectMapper();
        try {
            Files.createDirectories(this.macroDir);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar pasta para salvar os macros", e);
        }
    }

    /**
     * Salva uma definição de macro em arquivo JSON. Lança exceção caso já exista arquivo com o mesmo nome.
     */
    public void save(MacroSettingDTO settingDTO) throws IOException {
        String rawName = settingDTO.name().trim().toLowerCase();
        String nomeArquivo = rawName.endsWith(".json") ? rawName : rawName + ".json";
        if (existeTalArquivo(nomeArquivo)) {
            throw new FileSystemException("Já existe arquivo com este nome");
        }
        Path arquivoDestino = macroDir.resolve(nomeArquivo);
        objectMapper.writeValue(arquivoDestino.toFile(), settingDTO);
    }

    /**
     * Carrega e desserializa uma macro a partir de seu nome de arquivo.
     */
    public MacroSettingDTO load(String nomeArquivo) throws IOException {
        String arquivoFinal = nomeArquivo.endsWith(".json") ? nomeArquivo : nomeArquivo + ".json";
        Path caminhoDestino = macroDir.resolve(arquivoFinal);
        return objectMapper.readValue(caminhoDestino.toFile(), MacroSettingDTO.class);
    }

    public boolean existeTalArquivo(String nomeArquivo) {
        String arquivoFinal = nomeArquivo.endsWith(".json") ? nomeArquivo : nomeArquivo + ".json";
        return Files.exists(macroDir.resolve(arquivoFinal));
    }

    /**
     * Lista os nomes de todas as macros salvas no diretório, ordenadas alfabeticamente.
     */
    public List<String> listAllSettings() throws IOException {
        try (var stream = Files.list(this.macroDir)) {
            return stream
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString().replace(".json", ""))
                    .sorted()
                    .toList();
        }
    }
}
