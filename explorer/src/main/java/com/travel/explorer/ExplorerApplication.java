package com.travel.explorer;

import com.travel.explorer.config.DatabaseInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ExplorerApplication {

    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(ExplorerApplication.class);
        application.addInitializers(new DatabaseInitializer());
        application.run(args);
    }
}
