package com.travel.explorer.config;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/** Creates the database after configuration is resolved, before JPA and Flyway start. */
public class DatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        DataSourceProperties datasource = Binder.get(context.getEnvironment())
                .bind("spring.datasource", DataSourceProperties.class)
                .orElseGet(DataSourceProperties::new);
        ensureDatabaseExists(datasource);
    }

    private void ensureDatabaseExists(DataSourceProperties datasource) {
        try {
            String url = datasource.getUrl();
            String username = datasource.getUsername();
            String password = datasource.getPassword();

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
