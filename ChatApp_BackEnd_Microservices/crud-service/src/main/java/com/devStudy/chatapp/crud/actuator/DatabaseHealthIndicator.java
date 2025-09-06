package com.devStudy.chatapp.crud.actuator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
public class DatabaseHealthIndicator implements HealthIndicator {
    private final DataSource dataSource;

    @Autowired
    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        return getHealth(dataSource);
    }

    public static Health getHealth(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(3)) {
                return Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("status", "Connected")
                        .withDetail("validationQuery", "SELECT 1")
                        .build();
            }
        } catch (SQLException e) {
            return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("error", e.getMessage())
                    .build();
        }
        return Health.down().withDetail("database", "PostgreSQL").build();
    }
}