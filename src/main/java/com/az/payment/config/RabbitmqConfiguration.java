package com.az.payment.config;


import com.az.payment.constants.Exchnages;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitmqConfiguration {

//    @Bean("messageBus")
//    public Exchange createExchange() {
//        return new TopicExchange(Exchnages.MESSAGE_BUS);
//    }

//    @Bean
//    public Jackson2JsonMessageConverter jsonMessageConverter() {
//        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
//
//        return jackson2JsonMessageConverter;
//    }

//    @Bean
//    RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
//        RabbitTemplate rabbitTemplate = new RabbitTemplate();
//        rabbitTemplate.setConnectionFactory(connectionFactory);
//        rabbitTemplate.setMessageConverter(messageConverter(new ObjectMapper()));
//
//        return rabbitTemplate;
//    }
//
//    @Bean
//    public MessageConverter messageConverter(ObjectMapper jsonMapper) {
//        return new Jackson2JsonMessageConverter(jsonMapper);
//    }

}
