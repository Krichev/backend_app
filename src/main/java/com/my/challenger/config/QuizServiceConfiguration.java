package com.my.challenger.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class QuizServiceConfiguration {
    
//    @Bean
//    public QuizSessionBuilderService quizSessionBuilderService(QuizRoundRepository quizRoundRepository) {
//        return new QuizSessionBuilderService(quizRoundRepository);
//    }
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}