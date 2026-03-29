package com.travel.explorer.config;

import java.util.Random;
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
  public Random getRandom (){
    return new Random();
  }

}
