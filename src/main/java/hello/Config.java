package hello;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper result = new ObjectMapper();
        result.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        result.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        result.registerModule(javaTimeModule);
        return result;
    }
}
