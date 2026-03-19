package com.gymplatform.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String MEMBER_EXCHANGE = "member.events";
    public static final String PAYMENT_EXCHANGE = "payment.events";
    public static final String NOTIFICATION_EXCHANGE = "notification.events";

    @Bean
    public TopicExchange memberExchange() {
        return new TopicExchange(MEMBER_EXCHANGE);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public TopicExchange notificationExchange() {
        return new TopicExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue memberCreatedQueue() {
        return QueueBuilder.durable("member.created.queue").build();
    }

    @Bean
    public Queue paymentFailedQueue() {
        return QueueBuilder.durable("payment.failed.queue").build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable("notification.send.queue").build();
    }

    @Bean
    public Binding memberCreatedBinding() {
        return BindingBuilder.bind(memberCreatedQueue())
                .to(memberExchange()).with("member.created");
    }

    @Bean
    public Binding paymentFailedBinding() {
        return BindingBuilder.bind(paymentFailedQueue())
                .to(paymentExchange()).with("payment.failed");
    }

    @Bean
    public Binding notificationBinding() {
        return BindingBuilder.bind(notificationQueue())
                .to(notificationExchange()).with("notification.#");
    }

    @Bean
    public Queue memberCheckedInQueue() {
        return QueueBuilder.durable("member.checkedin.queue").build();
    }

    @Bean
    public Binding memberCheckedInBinding() {
        return BindingBuilder.bind(memberCheckedInQueue())
                .to(memberExchange()).with("member.checkedin");
    }

    @Bean
    public Queue notificationEventsQueue() {
        return QueueBuilder.durable("notification.events.queue").build();
    }

    @Bean
    public Binding notificationEventsBinding() {
        return BindingBuilder.bind(notificationEventsQueue())
                .to(notificationExchange()).with("#");
    }

    @Bean
    public Queue memberEventsNotificationQueue() {
        return QueueBuilder.durable("member.events.notification.queue").build();
    }

    @Bean
    public Binding memberEventsNotificationBinding() {
        return BindingBuilder.bind(memberEventsNotificationQueue())
                .to(memberExchange()).with("#");
    }

    @Bean
    public Queue paymentEventsNotificationQueue() {
        return QueueBuilder.durable("payment.events.notification.queue").build();
    }

    @Bean
    public Binding paymentEventsNotificationBinding() {
        return BindingBuilder.bind(paymentEventsNotificationQueue())
                .to(paymentExchange()).with("#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
