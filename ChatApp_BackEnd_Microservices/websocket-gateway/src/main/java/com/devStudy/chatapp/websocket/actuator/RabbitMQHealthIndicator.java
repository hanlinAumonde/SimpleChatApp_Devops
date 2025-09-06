package com.devStudy.chatapp.websocket.actuator;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class RabbitMQHealthIndicator implements HealthIndicator {
    private final RabbitTemplate rabbitTemplate;

    public RabbitMQHealthIndicator(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public Health health() {
        return getHealth(rabbitTemplate);
    }

    public static Health getHealth(RabbitTemplate rabbitTemplate) {
        try {
            rabbitTemplate.execute(channel -> channel.getConnection().isOpen());
            
            return Health.up()
                    .withDetail("rabbitmq", "Connected")
                    .withDetail("status", "UP")
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("rabbitmq", "Connection failed")
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }
}