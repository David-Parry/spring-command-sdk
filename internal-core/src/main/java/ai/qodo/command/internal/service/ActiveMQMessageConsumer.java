/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.internal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ActiveMQ message consumer that listens to messages from the configured topic
 * and routes them to appropriate services using the MessageEventRouter.
 * This implementation is activated when messaging.provider is set to "activemq".
 * 
 * Transaction support is enabled - messages will be automatically rolled back
 * and redelivered if an exception occurs during processing.
 */
@Service
@ConditionalOnProperty(name = "messaging.provider", havingValue = "activemq")
public class ActiveMQMessageConsumer {

    private static final Logger logger = LoggerFactory.getLogger(ActiveMQMessageConsumer.class);

    private final ApplicationContext applicationContext;


    public ActiveMQMessageConsumer(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @JmsListener(destination = "${messaging.queue.event}?consumer.exclusive=true")
    @Transactional("jmsTransactionManager")
    public void onEventMessage(String message) {
        process(message);
    }

    @JmsListener(destination = "${messaging.queue.response}?consumer.exclusive=true")
    @Transactional("jmsTransactionManager")
    public void onResponseMessage(String message) {
        process(message);
    }

    protected void process(String message) {
        logger.info("Received event message from ActiveMQ queue: {}", message.length() > 200 ?
                message.substring(0, 200) + "..." : message);
        logger.debug("Full event message content: {}", message);
        MessageRouter router = applicationContext.getBean(MessageRouter.class);
        router.processMessage(message);
        logger.debug("Successfully processed event message");
    }


}