package com.travel.explorer.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Run against a disposable PostgreSQL database using GEONAMES_TEST_DB_URL. */
@EnabledIfEnvironmentVariable(named = "GEONAMES_TEST_DB_URL", matches = ".+")
class GeonamesInitializerTest {
    private JdbcTemplate jdbc;
    private GeonamesInitializer initializer;

    @BeforeEach
    void setUp() {
        var dataSource = new DriverManagerDataSource(System.getenv("GEONAMES_TEST_DB_URL"),
                System.getenv().getOrDefault("GEONAMES_TEST_DB_USER", "postgres"),
                System.getenv().getOrDefault("GEONAMES_TEST_DB_PASSWORD", ""));
        jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("DROP TABLE IF EXISTS cities");
        jdbc.execute("DROP TABLE IF EXISTS contries");
        jdbc.execute("CREATE TABLE contries (id bigserial PRIMARY KEY, iso varchar(2) UNIQUE NOT NULL, name text)");
        jdbc.execute("CREATE TABLE cities (id bigserial PRIMARY KEY, name text, country_id bigint REFERENCES contries(id))");
        initializer = new GeonamesInitializer(dataSource);
    }

    @Test
    void importsBundledFilesAndRepairsMissingDataWithoutChangingExistingIds() throws Exception {
        initializer.run(null);
        int countries = count("contries");
        int cities = count("cities");
        assertThat(countries).isGreaterThan(200);
        assertThat(cities).isGreaterThan(20000);
        Long countryId = jdbc.queryForObject("SELECT id FROM contries WHERE iso = 'UA'", Long.class);
        Long cityId = jdbc.queryForObject("SELECT min(id) FROM cities WHERE country_id = ?", Long.class, countryId);
        initializer.run(null);
        assertThat(count("contries")).isEqualTo(countries);
        assertThat(count("cities")).isEqualTo(cities);
        jdbc.update("UPDATE contries SET name = 'incorrect' WHERE id = ?", countryId);
        jdbc.update("DELETE FROM cities WHERE id = (SELECT max(id) FROM cities WHERE id <> ?)", cityId);
        jdbc.update("DELETE FROM cities WHERE country_id IN (SELECT id FROM contries WHERE iso = 'AD')");
        jdbc.update("DELETE FROM contries WHERE iso = 'AD'");
        initializer.run(null);
        assertThat(count("contries")).isEqualTo(countries);
        assertThat(count("cities")).isEqualTo(cities);
        assertThat(jdbc.queryForObject("SELECT name FROM contries WHERE id = ?", String.class, countryId))
                .isEqualTo("Ukraine");
        assertThat(jdbc.queryForObject("SELECT country_id FROM cities WHERE id = ?", Long.class, cityId))
                .isEqualTo(countryId);
    }

    @Test
    void handlesUtf8CommentsEmptyColumnsAndDuplicateCityNames() throws Exception {
        var countries = resource("\uFEFF# comment\n\n" + row(0, "UA", 4, "Ukraine"));
        var cities = resource(row(1, "Київ", 8, "UA") + row(1, "Київ", 8, "UA")
                + row(1, "Unknown", 8, "ZZ"));
        initializer.initialize(countries, cities);
        initializer.initialize(countries, cities);
        assertThat(count("cities")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT name FROM cities", String.class)).isEqualTo("Київ");
    }

    @Test
    void rollsBackFailedImportAndAllowsRetry() throws Exception {
        var countries = resource(row(0, "UA", 4, "Ukraine"));
        assertThatThrownBy(() -> initializer.initialize(countries, resource("bad\trow\n")))
                .hasMessageContaining("19 tab-separated columns");
        assertThat(count("contries")).isZero();
        // Fail after countries were inserted, so transaction rollback is exercised too.
        jdbc.execute("ALTER TABLE cities ADD CONSTRAINT reject_city CHECK (name <> 'Rejected')");
        assertThatThrownBy(() -> initializer.initialize(countries, resource(row(1, "Rejected", 8, "UA"))))
                .isInstanceOf(java.sql.SQLException.class);
        assertThat(count("contries")).isZero();
        initializer.initialize(countries, resource(row(1, "Київ", 8, "UA")));
        assertThat(count("cities")).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_class WHERE relname IN "
                + "('temp_cities', 'temp_geonames_countries')", Integer.class)).isZero();
    }

    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private static String row(int first, String firstValue, int second, String secondValue) {
        String[] columns = new String[19];
        Arrays.fill(columns, "");
        columns[first] = firstValue;
        columns[second] = secondValue;
        return String.join("\t", columns) + "\n";
    }

    private static ByteArrayResource resource(String text) {
        return new ByteArrayResource(text.getBytes(StandardCharsets.UTF_8));
    }
}
