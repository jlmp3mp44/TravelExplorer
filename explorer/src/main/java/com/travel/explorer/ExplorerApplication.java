package com.travel.explorer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

@SpringBootApplication
public class ExplorerApplication {

	public static void main(String[] args) {
		ensureDatabaseExists();
		SpringApplication.run(ExplorerApplication.class, args);
	}

	private static void ensureDatabaseExists() {
		try {
			Properties props = PropertiesLoaderUtils.loadProperties(
					new ClassPathResource("application.properties"));
			Properties secrets = new Properties();
			try {
				secrets = PropertiesLoaderUtils.loadProperties(
						new ClassPathResource("application-secrets.properties"));
			} catch (Exception ignored) {
			}

			String url = props.getProperty("spring.datasource.url",
					secrets.getProperty("spring.datasource.url"));
			String username = secrets.getProperty("spring.datasource.username",
					props.getProperty("spring.datasource.username"));
			String password = secrets.getProperty("spring.datasource.password",
					props.getProperty("spring.datasource.password"));

			if (url == null || !url.startsWith("jdbc:postgresql://")) {
				return;
			}

			String dbName = url.substring(url.lastIndexOf('/') + 1);
			String serverUrl = url.substring(0, url.lastIndexOf('/')) + "/postgres";

			try (Connection conn = DriverManager.getConnection(serverUrl, username, password);
				 Statement stmt = conn.createStatement()) {
				ResultSet rs = stmt.executeQuery(
						"SELECT 1 FROM pg_database WHERE datname = '" + dbName + "'");
				if (!rs.next()) {
					stmt.executeUpdate("CREATE DATABASE " + dbName);
					System.out.println("Database '" + dbName + "' created successfully");
				}
			}
		} catch (Exception e) {
			System.err.println("Could not auto-create database: " + e.getMessage());
		}
	}
}
