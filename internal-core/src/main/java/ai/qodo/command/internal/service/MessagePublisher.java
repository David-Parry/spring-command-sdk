/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.internal.service;

/**
 * Interface for publishing messages to a topic.
 * This abstraction allows for different messaging implementations
 * to be configured through Spring Boot configuration.
 */
public interface MessagePublisher {
    String MSG_TYPE = "type";
    /**
     * Publishes a message to the configured topic.
     * 
     * @param message The message to publish
     */
    void publish(String message);

    /**
     * Publishes a message to the event topic.
     *
     * @param message The message to publish
     */
    void publishEvent(String message);

    /**
     * Publishes a message to the response topic.
     *
     * @param message The message to publish
     */
    void publishResponse(String message);



    /**
     * Publishes a message to a specific topic.
     * 
     * @param topic The topic to publish to
     * @param message The message to publish
     */
    void publish(String topic, String message);
}