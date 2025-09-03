package com.devStudy.chatapp.websocket.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Name of the exchange
    public static final String EXCHANGE_NAME = "chatroom-event-exchange";
    
    // Name of the queues
    public static final String WEBSOCKET_CHATROOM_MEMBER_CHANGE_QUEUE = "websocket.chatroom.member.change";
    public static final String WEBSOCKET_CHATROOM_REMOVE_QUEUE = "websocket.chatroom.remove";
    
    // 路由密钥
    public static final String CHATROOM_MEMBER_CHANGE_ROUTING_KEY = "chatroom.member.change";
    public static final String CHATROOM_REMOVE_ROUTING_KEY = "chatroom.remove";

    /**
     * Declare a topic exchange for chatroom events
     */
    @Bean
    public TopicExchange chatroomEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    /**
     * Declare WebSocket chatroom member change queue
     */
    @Bean
    public Queue websocketChatroomMemberChangeQueue() {
        return QueueBuilder.durable(WEBSOCKET_CHATROOM_MEMBER_CHANGE_QUEUE).build();
    }

    /**
     * Declare WebSocket chatroom remove queue
     */
    @Bean
    public Queue websocketChatroomRemoveQueue() {
        return QueueBuilder.durable(WEBSOCKET_CHATROOM_REMOVE_QUEUE).build();
    }

    /**
     * Binding chatroom member change queue to exchange
     */
    @Bean
    public Binding websocketChatroomMemberChangeBinding() {
        return BindingBuilder
                .bind(websocketChatroomMemberChangeQueue())
                .to(chatroomEventsExchange())
                .with(CHATROOM_MEMBER_CHANGE_ROUTING_KEY);
    }

    /**
     * Binding chatroom remove queue to exchange
     */
    @Bean
    public Binding websocketChatroomRemoveBinding() {
        return BindingBuilder
                .bind(websocketChatroomRemoveQueue())
                .to(chatroomEventsExchange())
                .with(CHATROOM_REMOVE_ROUTING_KEY);
    }

    /**
     * JSON message converter for RabbitMQ
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * Configuration of RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    /**
     * Configuration of RabbitListenerContainerFactory
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter());
        factory.setConcurrentConsumers(3);
        factory.setMaxConcurrentConsumers(10);
        return factory;
    }
}