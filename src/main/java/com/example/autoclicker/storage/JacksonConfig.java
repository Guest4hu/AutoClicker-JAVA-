package com.example.autoclicker.storage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Provedor de configuração centralizada para serialização e desserialização JSON via Jackson.
 */
public class JacksonConfig {

    private static final ObjectMapper INSTANCE = createObjectMapper();

    private JacksonConfig() {
    }

    private static ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModules(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    public static ObjectMapper getObjectMapper() {
        return INSTANCE;
    }
}
