package com.travel.explorer.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Reconciles the location catalogue after Hibernate has created/updated its tables. */
@Component
@DependsOn("entityManagerFactory")
public class GeonamesInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(GeonamesInitializer.class);
    private final DataSource dataSource;

    public GeonamesInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initialize(new ClassPathResource("db/countryInfo.csv"),
                new ClassPathResource("db/cities15000.csv"));
    }

    void initialize(Resource countries, Resource cities) throws SQLException, IOException {
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement()) {
                // Serialize concurrent application starts and catalogue writes.
                statement.execute("LOCK TABLE contries, cities IN SHARE ROW EXCLUSIVE MODE");
                load(connection, "temp_geonames_countries", countries);
                load(connection, "temp_cities", cities);
                int updated = statement.executeUpdate("""
                        UPDATE contries c SET name = t.col5
                        FROM temp_geonames_countries t
                        WHERE c.iso = t.col1 AND c.name IS DISTINCT FROM t.col5
                        """);
                int addedCountries = statement.executeUpdate("""
                        INSERT INTO contries (iso, name)
                        SELECT DISTINCT t.col1, t.col5 FROM temp_geonames_countries t
                        WHERE NOT EXISTS (SELECT 1 FROM contries c WHERE c.iso = t.col1)
                        """);
                // The application identifies cities by name and country (no GeoNames ID).
                int addedCities = statement.executeUpdate("""
                        INSERT INTO cities (name, country_id)
                        SELECT DISTINCT t.col2, c.id
                        FROM temp_cities t JOIN contries c ON t.col9 = c.iso
                        WHERE NOT EXISTS (
                            SELECT 1 FROM cities existing
                            WHERE existing.name = t.col2 AND existing.country_id = c.id
                        )
                        """);
                connection.commit();
                log.info("GeoNames catalogue: {} countries added, {} country names updated, {} cities added",
                        addedCountries, updated, addedCities);
            } catch (SQLException | IOException | RuntimeException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    private void load(Connection connection, String table, Resource resource) throws SQLException, IOException {
        String columns = IntStream.rangeClosed(1, 19)
                .mapToObj(i -> "col" + i + " text").collect(Collectors.joining(", "));
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TEMP TABLE " + table + " (" + columns + ") ON COMMIT DROP");
        }
        String placeholders = String.join(",", Collections.nCopies(19, "?"));
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resource.getInputStream(), StandardCharsets.UTF_8));
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO " + table + " VALUES (" + placeholders + ")")) {
            String line;
            int lineNumber = 0;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (lineNumber == 1 && line.startsWith("\uFEFF")) {
                    line = line.substring(1);
                }
                if (line.isBlank() || line.startsWith("#")) {
                    continue;
                }
                String[] values = line.split("\t", -1);
                if (values.length != 19) {
                    throw new IOException(resource.getDescription() + ": line " + lineNumber
                            + " must contain 19 tab-separated columns, found " + values.length);
                }
                for (int i = 0; i < values.length; i++) {
                    insert.setString(i + 1, values[i]);
                }
                insert.addBatch();
                if (++count % 500 == 0) {
                    insert.executeBatch();
                }
            }
            if (count % 500 != 0) {
                insert.executeBatch();
            }
            if (count == 0) {
                throw new IOException(resource.getDescription() + " contains no GeoNames records");
            }
        }
    }
}
