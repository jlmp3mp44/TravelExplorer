package com.travel.explorer.config;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

  @Bean
   public ModelMapper modelMapper() {
    return new ModelMapper();
  }

  @Bean
  public String openRouterApiKey(@Value("${spring.app.OPENROUTER_API_KEY}") String key) {
    return key;
  }

}
