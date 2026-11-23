package com.quizmaster;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.URI;

@SpringBootApplication
public class QuizMasterApplication {

    public static void main(String[] args) {
        useDatabaseUrlIfPresent();
        SpringApplication.run(QuizMasterApplication.class, args);
    }

    /**
     * Render / Railway / Heroku give the database as DATABASE_URL in the form
     * postgres://user:password@host:port/dbname. Convert it into Spring's JDBC settings.
     */
    static void useDatabaseUrlIfPresent() {
        String databaseUrl = System.getenv("DATABASE_URL");
        if (databaseUrl == null || databaseUrl.isBlank() || System.getenv("DB_URL") != null) return;
        URI uri = URI.create(databaseUrl.replaceFirst("^postgres(ql)?://", "postgresql://"));
        String[] userInfo = uri.getUserInfo() == null ? new String[0] : uri.getUserInfo().split(":", 2);
        int port = uri.getPort() == -1 ? 5432 : uri.getPort();
        System.setProperty("spring.datasource.url", "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath());
        if (userInfo.length > 0) System.setProperty("spring.datasource.username", userInfo[0]);
        if (userInfo.length > 1) System.setProperty("spring.datasource.password", userInfo[1]);
    }
}
