package com.atg.api.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Configuration
public class UtcDateTimeJacksonConfig {

    @Bean
    public JavaTimeModule utcDateTimeModule() {
        JavaTimeModule module = new JavaTimeModule();
        module.addSerializer(LocalDateTime.class, new JsonSerializer<>() {
            @Override
            public void serialize(LocalDateTime value, JsonGenerator generator,
                                 SerializerProvider serializers) throws IOException {
                generator.writeString(value.toInstant(ZoneOffset.UTC).toString());
            }
        });
        return module;
    }
}
