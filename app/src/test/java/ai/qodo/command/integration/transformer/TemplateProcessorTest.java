/*
 * Copyright (C) 2025 Qodo
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ai.qodo.command.integration.transformer;

import ai.qodo.command.internal.transformer.TemplateProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for TemplateProcessor functionality
 */
public class TemplateProcessorTest {
    
    private TemplateProcessor processor;
    
    @BeforeEach
    void setUp() {
        processor = new TemplateProcessor(new ObjectMapper());
    }
    
    @Test
    void testSimpleJsonPointerReplacement() throws Exception {
        String template = "Hello {/name}, you are {/age} years old!";
        String json = "{\"name\": \"John\", \"age\": 25}";
        
        String result = processor.processTemplate(template, json);
        
        assertEquals("Hello John, you are 25 years old!", result);
    }
    
    @Test
    void testNestedJsonPointerReplacement() throws Exception {
        String template = "User: {/user/name}, City: {/user/address/city}";
        String json = """
            {
                "user": {
                    "name": "Alice",
                    "address": {
                        "city": "New York",
                        "country": "USA"
                    }
                }
            }
            """;
        
        String result = processor.processTemplate(template, json);
        
        assertEquals("User: Alice, City: New York", result);
    }
    
    @Test
    void testDotNotationReplacement() throws Exception {
        String template = "Product: {product.name}, Price: ${product.price}";
        String json = """
            {
                "product": {
                    "name": "Laptop",
                    "price": 999.99
                }
            }
            """;
        
        String result = processor.processTemplate(template, json);
        
        assertEquals("Product: Laptop, Price: $999.99", result);
    }
    
    @Test
    void testArrayAccess() throws Exception {
        String template = "First item: {/items/0/name}, Second item: {/items/1/name}";
        String json = """
            {
                "items": [
                    {"name": "Item A"},
                    {"name": "Item B"}
                ]
            }
            """;
        
        String result = processor.processTemplate(template, json);
        
        assertEquals("First item: Item A, Second item: Item B", result);
    }
    
    @Test
    void testMissingValues() throws Exception {
        String template = "Name: {/name}, Missing: {/missing}";
        String json = "{\"name\": \"John\"}";
        
        String result = processor.processTemplate(template, json);
        
        assertEquals("Name: John, Missing: {/missing - NOT FOUND}", result);
    }
    
    @Test
    void testMixedNotations() throws Exception {
        String template = "JSON Pointer: {/user/name}, Dot notation: {user.email}";
        String json = """
            {
                "user": {
                    "name": "Bob",
                    "email": "bob@example.com"
                }
            }
            """;
        
        String result = processor.processTemplate(template, json);
        
        assertEquals("JSON Pointer: Bob, Dot notation: bob@example.com", result);
    }
    
    @Test
    void testComplexTemplate() throws Exception {
        String template = """
            Event: {/eventType}
            Timestamp: {/timestamp}
            Project: {/project/name} (ID: {/project/id})
            Issues: {/newIssues/0/title} - Severity: {/newIssues/0/severity}
            """;
        
        String json = """
            {
                "eventType": "security_alert",
                "timestamp": "2023-01-01T00:00:00Z",
                "project": {
                    "id": "proj-123",
                    "name": "My Project"
                },
                "newIssues": [
                    {
                        "title": "SQL Injection",
                        "severity": "high"
                    }
                ]
            }
            """;
        
        String result = processor.processTemplate(template, json);
        
        String expected = """
            Event: security_alert
            Timestamp: 2023-01-01T00:00:00Z
            Project: My Project (ID: proj-123)
            Issues: SQL Injection - Severity: high
            """;
        
        assertEquals(expected, result);
    }
    
    @Test
    void testEmptyTemplate() throws Exception {
        String template = "";
        String json = "{\"name\": \"John\"}";
        
        String result = processor.processTemplate(template, json);
        
        assertEquals("", result);
    }
    
    @Test
    void testTemplateWithoutMarkers() throws Exception {
        String template = "This is a plain text template without any markers.";
        String json = "{\"name\": \"John\"}";
        
        String result = processor.processTemplate(template, json);
        
        assertEquals("This is a plain text template without any markers.", result);
    }
}