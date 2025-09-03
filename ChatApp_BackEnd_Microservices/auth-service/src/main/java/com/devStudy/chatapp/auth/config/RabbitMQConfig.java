package com.devStudy.chatapp.auth.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${chatroomApp.rabbitmq.RABBITMQ_EXCHANGE_NAME}")
    private String RABBITMQ_EXCHANGE_NAME;

    @Value("${chatroomApp.rabbitmq.RABBITMQ_QUEUE_Q1}")
    private String RABBITMQ_QUEUE_Q1;

    @Value("${chatroomApp.rabbitmq.RABBITMQ_QUEUE_Q2}")
    private String RABBITMQ_QUEUE_Q2;

    @Value("${chatroomApp.rabbitmq.ROUTING_KEY_RET_PASSWORD}")
    private String ROUTING_KEY_RET_PASSWORD;

    @Value("${chatroomApp.rabbitmq.ROUTING_KEY_VERIFICATION_CODE}")
    private String ROUTING_KEY_VERIFICATION_CODE;

    @Bean 
    Queue queue() { return new Queue(RABBITMQ_QUEUE_Q1); }

    @Bean 
    Queue queue2() { 
        return new Queue(RABBITMQ_QUEUE_Q2); 
    }

    @Bean
    TopicExchange exchange() { 
        return new TopicExchange(RABBITMQ_EXCHANGE_NAME); 
    }

    @Bean
    Binding queueBinding(){
        return BindingBuilder.bind(queue()).to(exchange()).with(ROUTING_KEY_RET_PASSWORD);
    }

    @Bean
    Binding queueBinding2(){
        return BindingBuilder.bind(queue2()).to(exchange()).with(ROUTING_KEY_VERIFICATION_CODE);
    }
}