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

    // 交换机名称（与crud-service保持一致）
    public static final String EXCHANGE_NAME = "chatroom-event-exchange";
    
    // 队列名称
    public static final String WEBSOCKET_CHATROOM_MEMBER_CHANGE_QUEUE = "websocket.chatroom.member.change";
    public static final String WEBSOCKET_CHATROOM_REMOVE_QUEUE = "websocket.chatroom.remove";
    
    // 路由密钥
    public static final String CHATROOM_MEMBER_CHANGE_ROUTING_KEY = "chatroom.member.change";
    public static final String CHATROOM_REMOVE_ROUTING_KEY = "chatroom.remove";

    /**
     * 声明聊天室事件交换机
     */
    @Bean
    public TopicExchange chatroomEventsExchange() {
        return new TopicExchange(EXCHANGE_NAME, true, false);
    }

    /**
     * 声明WebSocket聊天室成员变更队列
     */
    @Bean
    public Queue websocketChatroomMemberChangeQueue() {
        return QueueBuilder.durable(WEBSOCKET_CHATROOM_MEMBER_CHANGE_QUEUE).build();
    }

    /**
     * 声明WebSocket聊天室删除队列
     */
    @Bean
    public Queue websocketChatroomRemoveQueue() {
        return QueueBuilder.durable(WEBSOCKET_CHATROOM_REMOVE_QUEUE).build();
    }

    /**
     * 绑定成员变更队列到交换机
     */
    @Bean
    public Binding websocketChatroomMemberChangeBinding() {
        return BindingBuilder
                .bind(websocketChatroomMemberChangeQueue())
                .to(chatroomEventsExchange())
                .with(CHATROOM_MEMBER_CHANGE_ROUTING_KEY);
    }

    /**
     * 绑定聊天室删除队列到交换机
     */
    @Bean
    public Binding websocketChatroomRemoveBinding() {
        return BindingBuilder
                .bind(websocketChatroomRemoveQueue())
                .to(chatroomEventsExchange())
                .with(CHATROOM_REMOVE_ROUTING_KEY);
    }

    /**
     * JSON消息转换器
     */
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter());
        return template;
    }

    /**
     * 配置监听器容器工厂
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