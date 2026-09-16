package com.travel.explorer.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class EnvironmentConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("spring.config.import=classpath:environment.properties")
            .withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                    new SystemEnvironmentPropertySource("testEnv", Map.of(
                            "DB_URL", "jdbc:postgresql://localhost:5432/env_database",
                            "DB_USERNAME", "env-user",
                            "DB_PASSWORD", "env-password",
                            "JWT_SECRET", "test-jwt-secret",
                            "GOOGLE_API_KEY", "test-google-key"))))
            .withInitializer(new ConfigDataApplicationContextInitializer());

    @Test
    void resolvesEnvironmentBeforeDatabaseInitialization() throws Exception {
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);
        ResultSet result = mock(ResultSet.class);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(result);
        when(result.next()).thenReturn(true);

        try (var driver = mockStatic(DriverManager.class)) {
            driver.when(() -> DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/postgres", "env-user", "env-password"))
                    .thenReturn(connection);

            runner.withInitializer(new DatabaseInitializer()).run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context.getEnvironment().getProperty("spring.app.jwtSecret"))
                        .isEqualTo("test-jwt-secret");
                assertThat(context.getEnvironment().getProperty("google.api.key"))
                        .isEqualTo("test-google-key");
                driver.verify(() -> DriverManager.getConnection(
                        "jdbc:postgresql://localhost:5432/postgres", "env-user", "env-password"));
                verify(statement).executeQuery("SELECT 1 FROM pg_database WHERE datname = 'env_database'");
            });
        }
    }

    @Test
    void honorsStandardSpringEnvironmentOverrides() {
        runner.withInitializer(context -> context.getEnvironment().getPropertySources().addFirst(
                new SystemEnvironmentPropertySource("springOverrides", Map.of(
                        "SPRING_DATASOURCE_URL", "jdbc:postgresql://test-db:5432/test_database",
                        "SPRING_DATASOURCE_USERNAME", "override-user",
                        "SPRING_DATASOURCE_PASSWORD", "override-password"))))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    var environment = context.getEnvironment();
                    assertThat(environment.getProperty("spring.datasource.url"))
                            .isEqualTo("jdbc:postgresql://test-db:5432/test_database");
                    assertThat(environment.getProperty("spring.datasource.username")).isEqualTo("override-user");
                    assertThat(environment.getProperty("spring.datasource.password")).isEqualTo("override-password");
                });
    }
}
