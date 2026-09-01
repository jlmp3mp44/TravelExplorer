package com.travel.explorer.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TestConfig {

  private static final Properties properties = new Properties();

  static {
    try (InputStream input = TestConfig.class
        .getClassLoader()
        .getResourceAsStream("test.properties")) {

      if (input == null) {
        throw new RuntimeException("test.properties not found");
      }

      properties.load(input);

    } catch (IOException e) {
      throw new RuntimeException("Failed to load test.properties", e);
    }
  }

  public static String getBaseUrl() {
    return properties.getProperty("base.url");
  }
}
