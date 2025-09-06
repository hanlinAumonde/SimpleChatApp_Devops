package com.devStudy.chatapp.message.actuator;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class MongoHealthIndicator implements HealthIndicator {
    private final MongoTemplate mongoTemplate;

    public MongoHealthIndicator(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public Health health() {
        return getHealth(mongoTemplate);
    }

    public static Health getHealth(MongoTemplate mongoTemplate) {
        try {
            mongoTemplate.getDb().listCollectionNames().first();
            
            return Health.up()
                    .withDetail("database", "MongoDB")
                    .withDetail("status", "Connected")
                    .withDetail("collections", mongoTemplate.getDb().listCollectionNames().into(new ArrayList<>()).size())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "MongoDB")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}